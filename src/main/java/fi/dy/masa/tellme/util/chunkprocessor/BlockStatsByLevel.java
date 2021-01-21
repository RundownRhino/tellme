package fi.dy.masa.tellme.util.chunkprocessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import com.google.common.collect.ArrayListMultimap;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.arguments.BlockStateParser;
import net.minecraft.item.ItemStack;
import net.minecraft.state.Property;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.registries.ForgeRegistries;
import fi.dy.masa.tellme.TellMe;
import fi.dy.masa.tellme.command.CommandUtils;
import fi.dy.masa.tellme.datadump.JERDYI.JEROreDistributionObject;
import fi.dy.masa.tellme.util.BlockInfo;
import fi.dy.masa.tellme.util.datadump.DataDump;
import fi.dy.masa.tellme.util.datadump.DataDump.Alignment;
import fi.dy.masa.tellme.util.datadump.DataDump.Format;
//import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

public class BlockStatsByLevel extends ChunkProcessorAllChunks {
    private final HashMap<BlockState, BlockStateCountByLevel> blockStats = new HashMap<>();
    private int chunkCount;
    private boolean append;
    private final HashMap<Integer, Long> areaScannedByLevel = new HashMap<>();

    public void setAppend(boolean append) {
        this.append = append;
    }

    public HashMap<Integer, Long> getAreaScannedByLevel() {
        return areaScannedByLevel;
    }

    @Override
    public void processChunks(Collection<Chunk> chunks, BlockPos posMin, BlockPos posMax) {
        final long timeBefore = System.nanoTime();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        final BlockState air = Blocks.AIR.getDefaultState();
        int count = 0;
        HashMap<Integer, Long> areasOnEachLevelThisProcess = new HashMap<>();
        if(!this.append){
            this.blockStats.clear();
        }
        for (Chunk chunk : chunks) {
            ChunkPos chunkPos = chunk.getPos();
            final int topY = chunk.getTopFilledSegment() + 15;
            final int xMin = Math.max(chunkPos.x << 4, posMin.getX());
            final int yMin = Math.max(0, posMin.getY());
            final int zMin = Math.max(chunkPos.z << 4, posMin.getZ());
            final int xMax = Math.min((chunkPos.x << 4) + 15, posMax.getX());
            final int yMax = Math.min(topY, posMax.getY());
            final int zMax = Math.min((chunkPos.z << 4) + 15, posMax.getZ());
            for (int y = yMin; y <= yMax; ++y) {
                long area = ((long) zMax - zMin + 1) * (xMax - xMin + 1);
                areasOnEachLevelThisProcess.merge(y, area, Long::sum);
                for (int z = zMin; z <= zMax; ++z) {
                    for (int x = xMin; x <= xMax; ++x) {
                        pos.setPos(x, y, z);
                        BlockState state = chunk.getBlockState(pos);
                        BlockStateCountByLevel inner_map = blockStats.computeIfAbsent(state, k -> {
                            final Block block = k.getBlock();
                            final ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
                            return new BlockStateCountByLevel(k, id, new HashMap<Integer, Long>());
                        });
                        inner_map.addToCount(y, 1);
                        count++;
                    }
                }
            }
            // don't bother with air
            // // Add the amount of air that would be in non-existing chunk sections within
            // the
            // // given volume
            // if (topY < posMax.getY()) {
            // counts.addTo(air,
            // (posMax.getY() - Math.max(topY, posMin.getY() - 1)) * (xMax - xMin + 1) *
            // (zMax - zMin + 1));
            // }
        }
        areasOnEachLevelThisProcess.forEach((y, area) -> {
            areaScannedByLevel.merge(y, area, (oldarea, newarea) -> append ? oldarea + newarea : newarea);
        });
        this.chunkCount = this.append ? this.chunkCount + chunks.size() : chunks.size();

        TellMe.logger.info(String.format(Locale.US, "Counted %d blocks in %d chunks in %.4f seconds.", count,
                chunks.size(), (System.nanoTime() - timeBefore) / 1000000000D));

        // this.addParsedData(counts);
    }

    // private void addParsedData(Object2LongOpenHashMap<BlockState> counts) {
    // if (this.append == false) {
    // this.blockStats.clear();
    // }

    // for (final BlockState state : counts.keySet()) {
    // try {
    // final Block block = state.getBlock();
    // final ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
    // final long amount = counts.getLong(state);

    // if (id == null) {
    // TellMe.logger.warn("Non-registered block: class = {}, state = {}",
    // block.getClass().getName(),
    // state);
    // }

    // BlockStateCountByLevel info = this.blockStats.computeIfAbsent(state,
    // (s) -> new BlockStateCountByLevel(state, id, 0));

    // if (this.append) {
    // info.addToCount(amount);
    // } else {
    // info.setCounts(amount);
    // }
    // } catch (Exception e) {
    // TellMe.logger.error("Caught an exception while getting block names", e);
    // }
    // }
    // }

    private List<BlockStateCountByLevel> getFilteredData(List<String> filters) throws CommandSyntaxException {
        ArrayList<BlockStateCountByLevel> list = new ArrayList<>();
        ArrayListMultimap<Block, BlockStateCountByLevel> infoByBlock = ArrayListMultimap.create();
        DynamicCommandExceptionType exception = new DynamicCommandExceptionType(
                (type) -> new StringTextComponent("Invalid block state filter: '" + type + "'"));

        for (BlockStateCountByLevel info : this.blockStats.values()) {
            infoByBlock.put(info.state.getBlock(), info);
        }

        for (String filter : filters) {
            StringReader reader = new StringReader(filter);
            BlockStateParser parser = (new BlockStateParser(reader, false)).parse(false);
            BlockState state = parser.getState();

            if (state == null) {
                throw exception.create(filter);
            }

            Block block = state.getBlock();
            Map<Property<?>, Comparable<?>> parsedProperties = parser.getProperties();

            // No block state properties specified, get all states for this block
            if (parsedProperties.size() == 0) {
                list.addAll(infoByBlock.get(block));
            }
            // Exact state specified, only add that state
            else if (parsedProperties.size() == state.getValues().size()) {
                BlockStateCountByLevel info = this.blockStats.get(state);

                if (info != null) {
                    list.add(info);
                }
            }
            // Some properties specified, filter by those
            else {
                List<BlockStateCountByLevel> listIn = infoByBlock.get(block);

                // Accept states whose properties are not being filtered, or the value matches
                // the filter
                for (BlockStateCountByLevel info : listIn) {
                    if (BlockInfo.statePassesFilter(info.state, parsedProperties)) {
                        list.add(info);
                    }
                }
            }
        }

        return list;
    }

    public List<String> queryAll(Format format, CommandUtils.BlockStateGrouping grouping, boolean sortByCount)
            throws CommandSyntaxException {
        return this.query(format, grouping, sortByCount, null);
    }

    public List<String> query(Format format, CommandUtils.BlockStateGrouping grouping, boolean sortByCount,
            @Nullable List<String> filters) throws CommandSyntaxException {
        DataDump dump = new DataDump(3, format);
        List<BlockStateCountByLevel> list = new ArrayList<>();

        if (filters != null) {
            list.addAll(this.getFilteredData(filters));
        } else {
            list.addAll(this.blockStats.values());
        }

        if (grouping == CommandUtils.BlockStateGrouping.BY_BLOCK) {
            IdentityHashMap<Block, BlockStateCountByLevel> map = new IdentityHashMap<>();

            for (final BlockStateCountByLevel info : list) {
                BlockStateCountByLevel combined = map.computeIfAbsent(info.state.getBlock(),
                        (b) -> new BlockStateCountByLevel(info.state, info.id, new HashMap<Integer, Long>()));
                combined.addAnother(info.getCounts());
            }

            list.clear();
            list.addAll(map.values());
        }

        list.sort(sortByCount ? BlockStateCountByLevel.getCountComparator()
                : BlockStateCountByLevel.getAlphabeticComparator());
        long total = 0L;

        for (BlockStateCountByLevel info : list) {
            if (grouping == CommandUtils.BlockStateGrouping.BY_STATE) {
                dump.addData(BlockInfo.blockStateToString(info.state), info.displayName,
                        String.valueOf(info.total_count));
            } else {
                dump.addData(info.registryName, info.displayName, String.valueOf(info.total_count));
            }

            if (info.state.isAir() == false) {
                total += info.total_count;
            }
        }

        dump.addTitle("Registry name", "Display name", "Count");
        dump.addFooter(String.format("Block stats from an area touching %d chunks", this.chunkCount));
        dump.addFooter(String.format("The listed output contains %d non-air blocks", total));

        dump.setColumnProperties(2, Alignment.RIGHT, true); // count
        dump.setSort(sortByCount == false);

        return dump.getLines();
    }

    private static class BlockStateCountByLevel {
        public final BlockState state;
        public final ResourceLocation id;
        public final String registryName;
        public final String displayName;
        public HashMap<Integer, Long> counts;
        public long total_count;

        public BlockStateCountByLevel(BlockState state, ResourceLocation id, HashMap<Integer, Long> counts) {
            Block block = state.getBlock();
            ItemStack stack = new ItemStack(block);
            String displayName = stack.isEmpty() == false ? stack.getDisplayName().getString()
                    : (new TranslationTextComponent(block.getTranslationKey())).getString();

            this.state = state;
            this.id = id;
            this.registryName = id.toString();
            this.displayName = displayName;
            this.counts = counts;
            recalculateTotal();
        }

        public void addAnother(HashMap<Integer, Long> counts2) {
            // Add up the counts at each level.
            counts2.forEach((level, amount) -> addToCount(level, amount));
        }

        private void recalculateTotal() {
            this.total_count = counts.values().stream().reduce(0l, Long::sum);
        }

        public long getTotalCount() {
            return total_count;
        }

        public void addToCount(int level, long amount) {
            this.counts.merge(level, amount, Long::sum);
            this.total_count += amount;
        }

        public void setCounts(HashMap<Integer, Long> counts) {
            this.counts = counts;
            recalculateTotal();
        }

        public String getRegistryName() {
            return this.registryName;
        }

        public HashMap<Integer, Long> getCounts() {
            return this.counts;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((registryName == null) ? 0 : registryName.hashCode());
            result = prime * result + ((state == null) ? 0 : state.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            BlockStateCountByLevel other = (BlockStateCountByLevel) obj;
            if (registryName == null) {
                if (other.registryName != null)
                    return false;
            } else if (!registryName.equals(other.registryName))
                return false;
            if (state == null) {
                if (other.state != null)
                    return false;
            } else if (!state.equals(other.state))
                return false;
            return true;
        }

        public static Comparator<BlockStateCountByLevel> getAlphabeticComparator() {
            return Comparator.comparing(BlockStateCountByLevel::getRegistryName);
        }

        public static Comparator<BlockStateCountByLevel> getCountComparator() {
            return Comparator.comparingLong(BlockStateCountByLevel::getTotalCount).reversed()
                    .thenComparing(BlockStateCountByLevel::getRegistryName);
        }
    }

    public List<JEROreDistributionObject> generateJERDistribs() {
        List<JEROreDistributionObject> distributionList = new ArrayList<>();
        HashMap<Integer, Long> areas = getAreaScannedByLevel();
        blockStats.forEach((state, stats) -> {
            String name = stats.registryName;
            StringBuilder distrib = new StringBuilder();
            HashMap<Integer, Long> counts = stats.getCounts();
            int min_y = 0;
            int max_y = 127; // JER interpolates missing points, so we need the full array.
            for (int y = min_y; y <= max_y; y++) {
                long area = areas.get(y);
                long count = counts.getOrDefault(y, 0l);
                double freq = 0;
                if (area == 0) {
                    if (count != 0) {
                        System.err.println(
                                "[generateJERDistribs]Found a block with nonzero appearances over zero area, ignoring! Block:"
                                        + name + ",y-level:" + y);
                    }
                } else {
                    freq = (double) count / (double) area;
                    if (freq > 1.0) {
                        System.err.println(
                                "[generateJERDistribs]Something's wrong: found an y-level with more blocks than area. Block:"
                                        + name + ", y-level:" + y + ", count:" + count + ", area:" + area + ".");
                    }
                }

                distrib.append(y + "," + freq + ";");
            }
            String dim = "minecraft:overworld"; // just assuming, FIXME to handle different dimensions later
            JEROreDistributionObject finalDist = new JEROreDistributionObject(name, distrib.toString(), false, null,
                    dim);
            distributionList.add(finalDist);
        });
        return distributionList;
    }
}

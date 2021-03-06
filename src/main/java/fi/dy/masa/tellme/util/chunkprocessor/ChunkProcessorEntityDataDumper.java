package fi.dy.masa.tellme.util.chunkprocessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.chunk.Chunk;
import fi.dy.masa.tellme.TellMe;
import fi.dy.masa.tellme.util.datadump.DataDump;

public class ChunkProcessorEntityDataDumper extends ChunkProcessorBase
{
    private final List<EntityDataEntry> data = new ArrayList<>();
    private final Set<EntityType<?>> filters = new HashSet<>();

    public ChunkProcessorEntityDataDumper(DataDump.Format format, Collection<String> filtersIn)
    {
        super(format);

        this.setFilters(filtersIn);
    }

    private void setFilters(Collection<String> filtersIn)
    {
        this.filters.clear();

        for (String str : filtersIn)
        {
            try
            {
                ResourceLocation id = new ResourceLocation(str);
                Optional<EntityType<?>> type = Registry.ENTITY_TYPE.getOptional(id);

                if (type.isPresent())
                {
                    this.filters.add(type.get());
                }
            }
            catch (Exception e)
            {
                TellMe.logger.warn("Invalid entity name '{}'", str);
            }
        }
    }

    @Override
    public void processChunk(Chunk chunk)
    {
        ClassInheritanceMultiMap<Entity>[] entityLists = chunk.getEntityLists();
        Set<EntityType<?>> filters = this.filters;
        boolean noFilters = filters.isEmpty();
        Vector3d min = this.minPos;
        Vector3d max = this.maxPos;
        boolean hasBox = min != null && max != null;
        double minX = min != null ? min.x : 0;
        double minY = min != null ? min.y : 0;
        double minZ = min != null ? min.z : 0;
        double maxX = max != null ? max.x : 0;
        double maxY = max != null ? max.y : 0;
        double maxZ = max != null ? max.z : 0;
        int total = 0;

        for (ClassInheritanceMultiMap<Entity> entityList : entityLists)
        {
            for (Entity entity : entityList)
            {
                Vector3d pos = entity.getPositionVec();

                if (hasBox &&
                            (pos.x < minX ||
                                     pos.y < minY ||
                                     pos.z < minZ ||
                                     pos.x > maxX ||
                                     pos.y > maxY ||
                                     pos.z > maxZ))
                {
                    continue;
                }

                EntityType<?> type = entity.getType();

                if (noFilters || filters.contains(type))
                {
                    ResourceLocation id = Registry.ENTITY_TYPE.getKey(type);
                    CompoundNBT tag = new CompoundNBT();

                    if (entity.writeUnlessRemoved(tag))
                    {
                        this.data.add(new EntityDataEntry(pos, id.toString(), tag.toString()));
                        ++total;
                    }
                }
            }
        }
    }

    @Override
    public DataDump getDump()
    {
        DataDump dump = new DataDump(3, this.format);

        dump.setSort(false);
        dump.addTitle("Position", "ID", "NBT Data");

        for (EntityDataEntry entry : this.data)
        {
            Vector3d pos = entry.pos;
            dump.addData(String.format("%.2f %.2f %.2f", pos.x, pos.y, pos.z), entry.entityId, entry.nbtData);
        }

        return dump;
    }

    private static class EntityDataEntry
    {
        public final Vector3d pos;
        public final String entityId;
        public final String nbtData;

        public EntityDataEntry(Vector3d pos, String entityId, String nbtData)
        {
            this.pos = pos;
            this.entityId = entityId;
            this.nbtData = nbtData;
        }
    }
}

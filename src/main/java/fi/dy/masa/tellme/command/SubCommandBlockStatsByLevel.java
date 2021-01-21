package fi.dy.masa.tellme.command;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import com.google.gson.Gson;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.DimensionArgument;
import net.minecraft.command.arguments.ILocationArgument;
import net.minecraft.command.arguments.Vec2Argument;
import net.minecraft.command.arguments.Vec3Argument;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;
import fi.dy.masa.tellme.TellMe;
import fi.dy.masa.tellme.command.CommandUtils.AreaType;
import fi.dy.masa.tellme.command.CommandUtils.IWorldRetriever;
import fi.dy.masa.tellme.command.CommandUtils.OutputType;
import fi.dy.masa.tellme.command.argument.BlockStateCountGroupingArgument;
import fi.dy.masa.tellme.command.argument.OutputFormatArgument;
import fi.dy.masa.tellme.command.argument.OutputTypeArgument;
import fi.dy.masa.tellme.command.argument.StringCollectionArgument;
import fi.dy.masa.tellme.datadump.JERDYI.JEROreDistributionObject;
import fi.dy.masa.tellme.util.OutputUtils;
import fi.dy.masa.tellme.util.chunkprocessor.BlockStatsByLevel;
import fi.dy.masa.tellme.util.datadump.DataDump;

public class SubCommandBlockStatsByLevel {
        private static final Map<UUID, BlockStatsByLevel> BLOCK_STATS = new HashMap<>();
        private static final BlockStatsByLevel CONSOLE_BLOCK_STATS = new BlockStatsByLevel();

        public static CommandNode<CommandSource> registerSubCommand(CommandDispatcher<CommandSource> dispatcher) {
                LiteralCommandNode<CommandSource> subCommandRootNode = Commands.literal("block-stats-by-level")
                                .executes(c -> printHelp(c.getSource())).build();

                subCommandRootNode.addChild(createCountNodes("count", false));
                subCommandRootNode.addChild(createCountNodes("count-append", true));
                subCommandRootNode.addChild(createOutputDataNodes());
                subCommandRootNode.addChild(createJERFileOutputDataNodes());

                return subCommandRootNode;
        }

        private static LiteralCommandNode<CommandSource> createCountNodes(String command, boolean isAppend) {
                LiteralCommandNode<CommandSource> actionNodeCount = Commands.literal(command).build();

                actionNodeCount.addChild(createCountNodeAllLoadedChunks(isAppend));
                actionNodeCount.addChild(createCountNodeArea(isAppend));
                actionNodeCount.addChild(createCountNodeBox(isAppend));
                actionNodeCount.addChild(createCountNodeRange(isAppend));

                return actionNodeCount;
        }

        private static int outputJERFile(CommandSource source) {
                BlockStatsByLevel stats = getBlockStatsByLevelFor(source.getEntity());
                List<JEROreDistributionObject> distribs = stats.generateJERDistribs();
                OutputUtils.printOutput(Arrays.asList(new Gson().toJson(distribs)), OutputType.FILE,
                                DataDump.Format.ASCII, "world-gen", source);
                return 1;
        }

        // tellme create-jer-file
        private static LiteralCommandNode<CommandSource> createJERFileOutputDataNodes() {
                LiteralCommandNode<CommandSource> actionNodeCreateJERFile = Commands.literal("create-jer-file")
                                .executes(c -> outputJERFile(c.getSource())).build();
                return actionNodeCreateJERFile;
        }

        // tellme output-data <to-chat | to-console | to-file> <ascii | csv>
        // [sort-by-count] [modid:block] [modid:block] ...
        private static LiteralCommandNode<CommandSource> createOutputDataNodes() {
                LiteralCommandNode<CommandSource> actionNodeOutputData = Commands.literal("output-data").build();

                ArgumentCommandNode<CommandSource, OutputType> argOutputType = Commands
                                .argument("output_type", OutputTypeArgument.create()).build();

                ArgumentCommandNode<CommandSource, DataDump.Format> argOutputFormat = Commands
                                .argument("output_format", OutputFormatArgument.create()).build();

                ArgumentCommandNode<CommandSource, CommandUtils.BlockStateGrouping> argDataGrouping = Commands
                                .argument("result_grouping", BlockStateCountGroupingArgument.create())
                                .executes(c -> outputData(c.getSource(), c.getArgument("output_type", OutputType.class),
                                                c.getArgument("output_format", DataDump.Format.class),
                                                c.getArgument("result_grouping", CommandUtils.BlockStateGrouping.class),
                                                false))
                                .build();

                LiteralCommandNode<CommandSource> argSortByCount = Commands.literal("sort-by-count")
                                .executes(c -> outputData(c.getSource(), c.getArgument("output_type", OutputType.class),
                                                c.getArgument("output_format", DataDump.Format.class),
                                                c.getArgument("result_grouping", CommandUtils.BlockStateGrouping.class),
                                                true))
                                .build();

                LiteralCommandNode<CommandSource> argSortByName = Commands.literal("sort-by-name")
                                .executes(c -> outputData(c.getSource(), c.getArgument("output_type", OutputType.class),
                                                c.getArgument("output_format", DataDump.Format.class),
                                                c.getArgument("result_grouping", CommandUtils.BlockStateGrouping.class),
                                                false))
                                .build();

                @SuppressWarnings("unchecked")
                ArgumentCommandNode<CommandSource, List<String>> argBlockFiltersSortByCount = Commands
                                .argument("block_filters",
                                                StringCollectionArgument.create(() -> ForgeRegistries.BLOCKS.getKeys()
                                                                .stream().map(ResourceLocation::toString)
                                                                .collect(Collectors.toList()), ""))
                                .executes(c -> outputData(c.getSource(), c.getArgument("output_type", OutputType.class),
                                                c.getArgument("output_format", DataDump.Format.class),
                                                c.getArgument("result_grouping", CommandUtils.BlockStateGrouping.class),
                                                true, c.getArgument("block_filters", List.class)))
                                .build();

                @SuppressWarnings("unchecked")
                ArgumentCommandNode<CommandSource, List<String>> argBlockFiltersSortByName = Commands
                                .argument("block_filters",
                                                StringCollectionArgument.create(() -> ForgeRegistries.BLOCKS.getKeys()
                                                                .stream().map(ResourceLocation::toString)
                                                                .collect(Collectors.toList()), ""))
                                .executes(c -> outputData(c.getSource(), c.getArgument("output_type", OutputType.class),
                                                c.getArgument("output_format", DataDump.Format.class),
                                                c.getArgument("result_grouping", CommandUtils.BlockStateGrouping.class),
                                                false, c.getArgument("block_filters", List.class)))
                                .build();

                actionNodeOutputData.addChild(argOutputType);
                argOutputType.addChild(argOutputFormat);
                argOutputFormat.addChild(argDataGrouping);

                argDataGrouping.addChild(argSortByCount);
                argSortByCount.addChild(argBlockFiltersSortByCount);

                argDataGrouping.addChild(argSortByName);
                argSortByName.addChild(argBlockFiltersSortByName);

                return actionNodeOutputData;
        }

        private static LiteralCommandNode<CommandSource> createCountNodeAllLoadedChunks(boolean isAppend) {
                LiteralCommandNode<CommandSource> argAreaType = Commands.literal(AreaType.LOADED.getArgument())
                                .executes(c -> countBlocksLoadedChunks(c.getSource(),
                                                CommandUtils::getWorldFromCommandSource, isAppend))
                                .build();

                ArgumentCommandNode<CommandSource, ResourceLocation> argDimension = Commands
                                .argument("dimension", DimensionArgument.getDimension())
                                .executes(c -> countBlocksLoadedChunks(c.getSource(),
                                                (s) -> DimensionArgument.getDimensionArgument(c, "dimension"),
                                                isAppend))
                                .build();

                argAreaType.addChild(argDimension);

                return argAreaType;
        }

        private static LiteralCommandNode<CommandSource> createCountNodeArea(boolean isAppend) {
                LiteralCommandNode<CommandSource> argAreaType = Commands.literal(AreaType.AREA.getArgument()).build();

                ArgumentCommandNode<CommandSource, ILocationArgument> argStartCorner = Commands
                                .argument("start_corner", Vec2Argument.vec2()).build();
                ArgumentCommandNode<CommandSource, ILocationArgument> argEndCorner = Commands
                                .argument("end_corner", Vec2Argument.vec2())
                                .executes(c -> countBlocksArea(c.getSource(), Vec2Argument.getVec2f(c, "start_corner"),
                                                Vec2Argument.getVec2f(c, "end_corner"),
                                                CommandUtils::getWorldFromCommandSource, isAppend))
                                .build();
                ArgumentCommandNode<CommandSource, ResourceLocation> argDimension = Commands
                                .argument("dimension", DimensionArgument.getDimension())
                                .executes(c -> countBlocksArea(c.getSource(), Vec2Argument.getVec2f(c, "start_corner"),
                                                Vec2Argument.getVec2f(c, "end_corner"),
                                                (s) -> DimensionArgument.getDimensionArgument(c, "dimension"),
                                                isAppend))
                                .build();

                argAreaType.addChild(argStartCorner);
                argStartCorner.addChild(argEndCorner);
                argEndCorner.addChild(argDimension);

                return argAreaType;
        }

        private static LiteralCommandNode<CommandSource> createCountNodeBox(boolean isAppend) {
                LiteralCommandNode<CommandSource> argAreaType = Commands.literal(AreaType.BOX.getArgument()).build();

                ArgumentCommandNode<CommandSource, ILocationArgument> argStartCorner = Commands
                                .argument("start_corner", Vec3Argument.vec3()).build();
                ArgumentCommandNode<CommandSource, ILocationArgument> argEndCorner = Commands
                                .argument("end_corner", Vec3Argument.vec3())
                                .executes(c -> countBlocksBox(c.getSource(), Vec3Argument.getVec3(c, "start_corner"),
                                                Vec3Argument.getVec3(c, "end_corner"),
                                                CommandUtils::getWorldFromCommandSource, isAppend))
                                .build();

                ArgumentCommandNode<CommandSource, ResourceLocation> argDimension = Commands
                                .argument("dimension", DimensionArgument.getDimension())
                                .executes(c -> countBlocksBox(c.getSource(), Vec3Argument.getVec3(c, "start_corner"),
                                                Vec3Argument.getVec3(c, "end_corner"),
                                                (s) -> DimensionArgument.getDimensionArgument(c, "dimension"),
                                                isAppend))
                                .build();

                argAreaType.addChild(argStartCorner);
                argStartCorner.addChild(argEndCorner);
                argEndCorner.addChild(argDimension);

                return argAreaType;
        }

        private static LiteralCommandNode<CommandSource> createCountNodeRange(boolean isAppend) {
                LiteralCommandNode<CommandSource> argAreaType = Commands.literal(AreaType.RANGE.getArgument()).build();

                ArgumentCommandNode<CommandSource, Integer> argChunkBlockRange = Commands
                                .argument("block_range", IntegerArgumentType.integer(0, 8192))
                                .executes(c -> countBlocksRange(c.getSource(),
                                                IntegerArgumentType.getInteger(c, "block_range"),
                                                CommandUtils.getVec3dFromSource(c.getSource()),
                                                CommandUtils::getWorldFromCommandSource, isAppend))
                                .build();
                ArgumentCommandNode<CommandSource, ILocationArgument> argCenter = Commands
                                .argument("center", Vec3Argument.vec3())
                                .executes(c -> countBlocksRange(c.getSource(),
                                                IntegerArgumentType.getInteger(c, "block_range"),
                                                CommandUtils.getVec3dFromArg(c, "center"),
                                                CommandUtils::getWorldFromCommandSource, isAppend))
                                .build();
                ArgumentCommandNode<CommandSource, ResourceLocation> argDimension = Commands
                                .argument("dimension", DimensionArgument.getDimension())
                                .executes(c -> countBlocksRange(c.getSource(),
                                                IntegerArgumentType.getInteger(c, "block_range"),
                                                CommandUtils.getVec3dFromArg(c, "center"),
                                                (s) -> DimensionArgument.getDimensionArgument(c, "dimension"),
                                                isAppend))
                                .build();

                argAreaType.addChild(argChunkBlockRange);
                argChunkBlockRange.addChild(argCenter);
                argCenter.addChild(argDimension);

                return argAreaType;
        }

        private static int countBlocksRange(CommandSource source, int range, Vector3d center,
                        IWorldRetriever dimensionGetter, boolean isAppend) throws CommandSyntaxException {
                BlockPos centerPos = new BlockPos(center);
                BlockPos minPos = new BlockPos(centerPos.getX() - range, Math.max(0, centerPos.getY() - range),
                                centerPos.getZ() - range);
                BlockPos maxPos = new BlockPos(centerPos.getX() + range, Math.min(255, centerPos.getY() + range),
                                centerPos.getZ() + range);

                return countBlocksBox(source, minPos, maxPos, dimensionGetter, isAppend);
        }

        private static int countBlocksBox(CommandSource source, Vector3d corner1, Vector3d corner2,
                        IWorldRetriever dimensionGetter, boolean isAppend) throws CommandSyntaxException {
                BlockPos minPos = CommandUtils.getMinCorner(corner1, corner2);
                BlockPos maxPos = CommandUtils.getMaxCorner(corner1, corner2);

                return countBlocksBox(source, minPos, maxPos, dimensionGetter, isAppend);
        }

        private static int countBlocksArea(CommandSource source, Vector2f corner1, Vector2f corner2,
                        IWorldRetriever dimensionGetter, boolean isAppend) throws CommandSyntaxException {
                BlockPos minPos = CommandUtils.getMinCorner(corner1, corner2);
                BlockPos maxPos = CommandUtils.getMaxCorner(corner1, corner2);

                return countBlocksBox(source, minPos, maxPos, dimensionGetter, isAppend);
        }

        private static int countBlocksBox(CommandSource source, BlockPos minPos, BlockPos maxPos,
                        IWorldRetriever dimensionGetter, boolean isAppend) throws CommandSyntaxException {
                World world = dimensionGetter.getWorldFromSource(source);
                BlockStatsByLevel blockStats = getBlockStatsFor(source.getEntity());

                CommandUtils.sendMessage(source, "Counting blocks...");

                blockStats.setAppend(isAppend);
                blockStats.processChunks(world, minPos, maxPos);

                CommandUtils.sendMessage(source, "Done");

                return 1;
        }

        private static int countBlocksLoadedChunks(CommandSource source, IWorldRetriever dimensionGetter,
                        boolean isAppend) throws CommandSyntaxException {
                World world = dimensionGetter.getWorldFromSource(source);
                BlockStatsByLevel blockStats = getBlockStatsFor(source.getEntity());

                CommandUtils.sendMessage(source, "Counting blocks...");

                blockStats.setAppend(isAppend);
                blockStats.processChunks(TellMe.dataProvider.getLoadedChunks(world));

                CommandUtils.sendMessage(source, "Done");

                return 1;
        }

        private static int printHelp(CommandSource source) {
                CommandUtils.sendMessage(source, "Calculates the number of blocks in a given area, by y-level");
                CommandUtils.sendMessage(source,
                                "Usage: /tellme block-stats-by-level count[-append] all-loaded-chunks [dimension]");
                CommandUtils.sendMessage(source,
                                "Usage: /tellme block-stats-by-level count[-append] area <x1> <z1> <x2> <z2> [dimension]");
                CommandUtils.sendMessage(source,
                                "Usage: /tellme block-stats-by-level count[-append] box <x1> <y1> <z1> <x2> <y2> <z2> [dimension]");
                CommandUtils.sendMessage(source,
                                "Usage: /tellme block-stats-by-level count[-append] range <block_range> [x y z (of the center)] [dimension]");
                CommandUtils.sendMessage(source,
                                "Usage: /tellme block-stats-by-level output-data <to-chat | to-console | to-file> <ascii | csv> <by-block | by-state> [sort-by-count] [modid:block] [modid:block] ...");
                CommandUtils.sendMessage(source,
                                "- count: Clears previously stored results, and then counts all the blocks in the given area");
                CommandUtils.sendMessage(source,
                                "- count-append: Counts all the blocks in the given area, appending the data to the previously stored results");
                CommandUtils.sendMessage(source,
                                "- output-data: Outputs the stored data from previous count operations to the selected output location.");
                CommandUtils.sendMessage(source,
                                "- output-data: The 'file' output's dump files will go to 'config/tellme/'.");
                CommandUtils.sendMessage(source,
                                "- output-data: If you give some block names, then only the data for those given blocks will be included in the output");

                return 1;
        }

        private static int outputData(CommandSource source, OutputType outputType, DataDump.Format format,
                        CommandUtils.BlockStateGrouping grouping, boolean sortByCount) throws CommandSyntaxException {
                return outputData(source, outputType, format, grouping, sortByCount, null);
        }

        private static int outputData(CommandSource source, OutputType outputType, DataDump.Format format,
                        CommandUtils.BlockStateGrouping grouping, boolean sortByCount, @Nullable List<String> filters)
                        throws CommandSyntaxException {
                BlockStatsByLevel blockStats = getBlockStatsFor(source.getEntity());
                List<String> lines;

                // We have some filters specified
                if (filters != null && filters.isEmpty() == false) {
                        lines = blockStats.query(format, grouping, sortByCount, filters);
                } else {
                        lines = blockStats.queryAll(format, grouping, sortByCount);
                }

                OutputUtils.printOutput(lines, outputType, format, "block_stats", source);

                return 1;
        }

        private static BlockStatsByLevel getBlockStatsFor(@Nullable Entity entity) {
                if (entity == null) {
                        return CONSOLE_BLOCK_STATS;
                }

                return BLOCK_STATS.computeIfAbsent(entity.getUniqueID(), (e) -> new BlockStatsByLevel());
        }

        private static BlockStatsByLevel getBlockStatsByLevelFor(@Nullable Entity entity) {
                if (entity == null) {
                        return CONSOLE_BLOCK_STATS;
                }

                return BLOCK_STATS.computeIfAbsent(entity.getUniqueID(), (e) -> new BlockStatsByLevel());
        }
}

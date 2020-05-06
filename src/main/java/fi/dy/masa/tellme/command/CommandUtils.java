package fi.dy.masa.tellme.command;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.arguments.Vec2Argument;
import net.minecraft.command.arguments.Vec3Argument;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.dimension.DimensionType;

public class CommandUtils
{
    public static final FilenameFilter FILTER_FILES = (pathName, fileName) -> { return (new File(pathName, fileName)).isFile(); };
    public static final SimpleCommandExceptionType NO_DIMENSION_EXCEPTION = new SimpleCommandExceptionType(new StringTextComponent("This command must either be executed by an entity, or the dimension must be specified"));
    public static final DynamicCommandExceptionType DIMENSION_NOT_LOADED_EXCEPTION = new DynamicCommandExceptionType((type) -> new StringTextComponent("The dimension \"" + type + "\" was not loaded"));
    public static final SimpleCommandExceptionType NOT_AN_ENTITY_EXCEPTION = new SimpleCommandExceptionType(new StringTextComponent("This command must be executed by an entity"));
    public static final DynamicCommandExceptionType INVALID_OUTPUT_TYPE_EXCEPTION = new DynamicCommandExceptionType((type) -> new StringTextComponent("Invalid output type: " + type));

    public static BlockPos getMinCorner(Vec2f pos1, Vec2f pos2)
    {
        return new BlockPos(Math.min(MathHelper.floor(pos1.x), MathHelper.floor(pos2.x)),
                            0,
                            Math.min(MathHelper.floor(pos1.y), MathHelper.floor(pos2.y)));
    }

    public static BlockPos getMaxCorner(Vec2f pos1, Vec2f pos2)
    {
        return new BlockPos(Math.max(MathHelper.floor(pos1.x), MathHelper.floor(pos2.x)),
                            255,
                            Math.max(MathHelper.floor(pos1.y), MathHelper.floor(pos2.y)));
    }

    public static BlockPos getMinCorner(Vec3d pos1, Vec3d pos2)
    {
        return new BlockPos(Math.min(MathHelper.floor(pos1.x), MathHelper.floor(pos2.x)),
                            Math.min(MathHelper.floor(pos1.y), MathHelper.floor(pos2.y)),
                            Math.min(MathHelper.floor(pos1.z), MathHelper.floor(pos2.z)));
    }

    public static BlockPos getMaxCorner(Vec3d pos1, Vec3d pos2)
    {
        return new BlockPos(Math.max(MathHelper.floor(pos1.x), MathHelper.floor(pos2.x)),
                            Math.max(MathHelper.floor(pos1.y), MathHelper.floor(pos2.y)),
                            Math.max(MathHelper.floor(pos1.z), MathHelper.floor(pos2.z)));
    }

    /**
     * Gets the given Vec2f as a ChunkPos, converting the coordinates from block to chunk coordinates
     * @param vec
     * @return
     */
    public static ChunkPos getAsChunkPos(Vec2f vec)
    {
        return new ChunkPos(MathHelper.floor(vec.x) >> 4, MathHelper.floor(vec.y) >> 4);
    }

    /**
     * Takes in block coordinates, outputs chunk coordinates
     * @param pos1
     * @param pos2
     * @return
     */
    public static ChunkPos getMinCornerChunkPos(Vec2f pos1, Vec2f pos2)
    {
        return new ChunkPos(Math.min(MathHelper.floor(pos1.x) >> 4, MathHelper.floor(pos2.x) >> 4),
                            Math.min(MathHelper.floor(pos1.y) >> 4, MathHelper.floor(pos2.y) >> 4));
    }

    /**
     * Takes in block coordinates, outputs chunk coordinates
     * @param pos1
     * @param pos2
     * @return
     */
    public static ChunkPos getMaxCornerChunkPos(Vec2f pos1, Vec2f pos2)
    {
        return new ChunkPos(Math.max(MathHelper.floor(pos1.x) >> 4, MathHelper.floor(pos2.x) >> 4),
                            Math.max(MathHelper.floor(pos1.y) >> 4, MathHelper.floor(pos2.y) >> 4));
    }

    public static CompletableFuture<Suggestions> suggestIterable(Iterable<String> iterable, SuggestionsBuilder builder)
    {
        String arg = builder.getRemaining().toLowerCase(Locale.ROOT);

        for (String str : iterable)
        {
            if (str.startsWith(arg))
            {
                builder.suggest(str);
            }
        }

        return builder.buildFuture();
    }

    public static List<String> getFileNames(File dir, FilenameFilter filter)
    {
        if (dir.isDirectory())
        {
            String[] names = dir.list(filter);
            List<String> files = Arrays.asList(names);
            files.sort((a, b) -> a.compareTo(b));
            return files;
        }

        return Collections.emptyList();
    }

    public static void throwException(String message) throws CommandSyntaxException
    {
        throw (new SimpleCommandExceptionType(new TranslationTextComponent(message))).create();
    }

    public static void sendMessage(CommandSource source, String message)
    {
        source.sendFeedback(new StringTextComponent(message), true);
    }

    public static Vec2f getVec2fFromSource(CommandSource source)
    {
        Entity entity = source.getEntity();
        return entity != null ? new Vec2f((float) entity.posX, (float) entity.posZ) : Vec2f.ZERO;
    }

    public static Vec2f getVec2fFromArg(CommandContext<CommandSource> ctx, String argName) throws CommandSyntaxException
    {
        return Vec2Argument.getVec2f(ctx, argName);
    }

    public static Vec3d getVec3dFromSource(CommandSource source)
    {
        Entity entity = source.getEntity();
        return entity != null ? entity.getPositionVector() : Vec3d.ZERO;
    }

    public static BlockPos getBlockPosFromSource(CommandSource source)
    {
        Entity entity = source.getEntity();
        return entity != null ? new BlockPos(entity.getPositionVec()) : BlockPos.ZERO;
    }

    public static Vec3d getVec3dFromArg(CommandContext<CommandSource> ctx, String argName) throws CommandSyntaxException
    {
        return Vec3Argument.getVec3(ctx, argName);
    }

    public static DimensionType getDimensionFromSource(CommandSource source) throws CommandSyntaxException
    {
        Entity entity = source.getEntity();

        if (entity == null)
        {
            throw NO_DIMENSION_EXCEPTION.create();
        }

        return entity.dimension;
    }

    public interface IDimensionRetriever
    {
        DimensionType getDimensionFromSource(CommandSource source) throws CommandSyntaxException;
    }

    public enum OutputType
    {
        CHAT        ("to-chat"),
        CONSOLE     ("to-console"),
        FILE        ("to-file");

        private final String arg;

        OutputType(String arg)
        {
            this.arg = arg;
        }

        public String getArgument()
        {
            return this.arg;
        }

        @Nullable
        public static OutputType fromArg(String arg)
        {
            for (OutputType type : OutputType.values())
            {
                if (type.arg.equals(arg))
                {
                    return type;
                }
            }

            return null;
        }
    }

    public enum AreaType
    {
        AREA            ("area"),
        BOX             ("box"),
        CHUNK_RADIUS    ("chunk-radius"),
        LOADED          ("loaded-chunks"),
        RANGE           ("range"),
        SAMPLED         ("sampled");

        private final String arg;

        AreaType(String arg)
        {
            this.arg = arg;
        }

        public String getArgument()
        {
            return this.arg;
        }

        @Nullable
        public static AreaType fromArg(String arg)
        {
            for (AreaType type : AreaType.values())
            {
                if (type.arg.equals(arg))
                {
                    return type;
                }
            }

            return null;
        }
    }
}
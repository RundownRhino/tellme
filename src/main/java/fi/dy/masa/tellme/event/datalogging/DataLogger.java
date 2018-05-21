package fi.dy.masa.tellme.event.datalogging;

import java.util.EnumMap;
import javax.annotation.Nullable;
import net.minecraft.entity.Entity;
import net.minecraft.world.chunk.Chunk;
import fi.dy.masa.tellme.datadump.DataDump;
import fi.dy.masa.tellme.event.datalogging.LoggerWrapperBase.OutputType;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class DataLogger
{
    private static final Int2ObjectOpenHashMap<DataLogger> INSTANCES = new Int2ObjectOpenHashMap<>();

    private final EnumMap<DataType, LoggerWrapper> loggers = new EnumMap<>(DataType.class);
    private final LoggerWrapperBase dummyWrapper = new LoggerWrapperBase(DataType.CHUNK_LOAD);
    private final int dimension;

    private DataLogger(int dimension)
    {
        this.dimension = dimension;
    }

    public static DataLogger instance(int dimension)
    {
        DataLogger logger = INSTANCES.get(dimension);

        if (logger == null)
        {
            logger = new DataLogger(dimension);
            INSTANCES.put(dimension, logger);
        }

        return logger;
    }

    public boolean setLoggingEnabled(DataType type, boolean enabled)
    {
        return this.setEnabled(OutputType.PRINT, type, enabled);
    }

    public boolean setPrintingEnabled(DataType type, boolean enabled)
    {
        return this.setEnabled(OutputType.PRINT, type, enabled);
    }

    private boolean setEnabled(OutputType outputType, DataType dataType, boolean enabled)
    {
        LoggerWrapperBase wrapper;
        boolean changed = false;

        if (enabled)
        {
            wrapper = this.getOrCreateLoggerWrapper(dataType);
        }
        else
        {
            wrapper = this.getLoggerWrapper(dataType);
        }

        changed = wrapper.isEnabled(outputType) != enabled;
        wrapper.setEnabled(outputType, enabled);

        this.updateEventHandlers(dataType, enabled);

        return changed;
    }

    private void updateEventHandlers(DataType type, boolean enabled)
    {
        if (enabled)
        {
            EventManager.registerHandler(type);
        }
        else
        {
            for (DataLogger logger : INSTANCES.values())
            {
                LoggerWrapper wrapper = logger.loggers.get(type);

                if (wrapper != null && (wrapper.enableLog || wrapper.enablePrint))
                {
                    return;
                }
            }

            EventManager.unregisterHandler(type);
        }
    }

    public void printLoggers(DataDump dump)
    {
        for (LoggerWrapper wrapper : this.loggers.values())
        {
            dump.addData(   String.valueOf(this.dimension),
                            wrapper.type.getOutputName(),
                            String.valueOf(wrapper.enablePrint),
                            String.valueOf(wrapper.enableLog));
        }
    }

    public void clearData(DataType type)
    {
        this.getLoggerWrapper(type).clearData();
    }

    public void dumpData(DataType type, DataDump.Format format)
    {
        this.getLoggerWrapper(type).dumpData(format);
    }

    public void onChunkEvent(DataType type, Chunk chunk)
    {
        this.getLoggerWrapper(type).onChunkEvent(type, chunk);
    }

    public void onEntityEvent(DataType type, Entity entity)
    {
        this.getLoggerWrapper(type).onEntityEvent(type, entity);
    }

    @Nullable
    private LoggerWrapperBase getLoggerWrapper(DataType type)
    {
        LoggerWrapperBase wrapper = this.loggers.get(type);
        return wrapper != null ? wrapper : this.dummyWrapper;
    }

    private LoggerWrapper getOrCreateLoggerWrapper(DataType type)
    {
        LoggerWrapper wrapper = this.loggers.get(type);

        if (wrapper == null)
        {
            wrapper = new LoggerWrapper(type);
            this.loggers.put(type, wrapper);
        }

        return wrapper;
    }

    public enum DataType
    {
        CHUNK_LOAD              ("chunk-load",          "Chunk Load"),
        CHUNK_UNLOAD            ("chunk-unload",        "Chunk Unload"),
        ENTITY_JOIN_WORLD       ("entity-join-world",   "Entity Join World");

        private final String argName;
        private final String outputName;

        private DataType(String argName, String outputName)
        {
            this.argName = argName;
            this.outputName = outputName;
        }

        public String getArgName()
        {
            return this.argName;
        }

        public String getOutputName()
        {
            return this.outputName;
        }

        @Nullable
        public static DataType fromArgument(String arg)
        {
            for (DataType type : DataType.values())
            {
                if (type.argName.equals(arg))
                {
                    return type;
                }
            }

            return null;
        }
    }
}
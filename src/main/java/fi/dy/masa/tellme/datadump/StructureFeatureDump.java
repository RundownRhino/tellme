package fi.dy.masa.tellme.datadump;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.StructureFeature;
import fi.dy.masa.tellme.mixin.IMixinWeightedPickerEntry;
import fi.dy.masa.tellme.util.datadump.DataDump;

public class StructureFeatureDump
{
    public static List<String> getFormattedDump(DataDump.Format format, boolean spawns)
    {
        DataDump dump = new DataDump(spawns ? 4 : 2, format);

        for (Identifier id : Registry.STRUCTURE_FEATURE.getIds())
        {
            StructureFeature<?> feature = Registry.STRUCTURE_FEATURE.get(id);

            if (spawns)
            {
                String mobSpawns = getMobSpawnsString(feature.getMonsterSpawns());
                String passiveSpawns = getMobSpawnsString(feature.getCreatureSpawns());
                dump.addData(id.toString(), feature.getName(), mobSpawns, passiveSpawns);
            }
            else
            {
                dump.addData(id.toString(), feature.getName());
            }
        }

        if (spawns)
        {
            dump.addTitle("Registry name", "Name", "Mob spawns", "Passive spawns");
        }
        else
        {
            dump.addTitle("Registry name", "Name");
        }

        return dump.getLines();
    }

    public static String getMobSpawnsString(Collection<Biome.SpawnEntry> list)
    {
        List<String> spawns = new ArrayList<>();

        for (Biome.SpawnEntry spawn : list)
        {
            Identifier erl = Registry.ENTITY_TYPE.getId(spawn.type);
            String entName = erl != null ? erl.toString() : "<null>";
            spawns.add(String.format("{ %s [weight: %d, min: %d, max: %d] }", entName, ((IMixinWeightedPickerEntry) spawn).getWeight(), spawn.minGroupSize, spawn.maxGroupSize));
        }

        Collections.sort(spawns);

        return String.join(", ", spawns);
    }
}
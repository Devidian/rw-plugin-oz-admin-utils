package de.omegazirkel.risingworld.adminutils.exports;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import net.risingworld.api.Plugin;

public final class AdminUtilsPluginListExportService {
    private static final int SCHEMA_VERSION = 1;

    public PluginListExport exportPlugins(Collection<PluginExport> plugins) {
        return exportPlugins(plugins, null);
    }

    public PluginListExport exportPlugins(Collection<PluginExport> plugins, Long lastChange) {
        List<PluginExport> sorted = plugins.stream()
                .sorted(Comparator.comparing(PluginExport::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparingInt(PluginExport::id))
                .toList();
        return new PluginListExport(SCHEMA_VERSION, System.currentTimeMillis(), sorted);
    }

    public PluginListExport exportRuntimePlugins(Collection<Plugin> plugins) {
        return exportRuntimePlugins(plugins, null);
    }

    public PluginListExport exportRuntimePlugins(Collection<Plugin> plugins, Long lastChange) {
        return exportPlugins(plugins.stream()
                .map(plugin -> new PluginExport(
                        plugin.getID(),
                        plugin.getName(),
                        plugin.getPath(),
                        plugin.getLoadOrder()))
                .toList(), lastChange);
    }
}

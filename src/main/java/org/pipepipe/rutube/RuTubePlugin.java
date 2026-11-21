package org.pipepipe.rutube;

import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.plugin.PipePipePlugin;
import org.schabi.newpipe.extractor.plugin.PluginContext;

import javax.annotation.Nonnull;

/** Entry point, referenced by {@code entryClass} in plugin.json. */
public final class RuTubePlugin implements PipePipePlugin {
    @Nonnull
    @Override
    public StreamingService createService(final int serviceId,
                                          @Nonnull final PluginContext context) {
        return new RuTubeService(serviceId, context.getPreferences());
    }
}

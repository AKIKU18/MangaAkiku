package com.example.mangav5.Sources;

import com.example.mangav5.Sources.Implementations.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SourceManager {
    private static SourceManager instance;
    private final Map<String, MangaSource> sources = new HashMap<>();

    private SourceManager() {
        registerSource(new AsuraSource());
        registerSource(new MangaDexSource());
        registerSource(new ManhuausSource());
        registerSource(new ManhuaPlusSource());
        registerSource(new DemonicScansSource());
        registerSource(new ManhuaFastSource());
        registerSource(new FlameComicsSource());
        registerSource(new RizzfablesSource());
        registerSource(new MgekoSource());
        registerSource(new ComixSource());
        registerSource(new VortexScansSource());
    }

    public static synchronized SourceManager getInstance() {
        if (instance == null) {
            instance = new SourceManager();
        }
        return instance;
    }

    public void registerSource(MangaSource source) {
        sources.put(source.getSourceName(), source);
    }

    public MangaSource getSource(String name) {
        return sources.get(name);
    }

    public Set<String> getAvailableSources() {
        return sources.keySet();
    }
}

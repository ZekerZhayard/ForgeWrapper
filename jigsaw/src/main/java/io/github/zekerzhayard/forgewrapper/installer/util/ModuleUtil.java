package io.github.zekerzhayard.forgewrapper.installer.util;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.module.ResolvedModule;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import sun.misc.Unsafe;

public class ModuleUtil {
    private final static MethodHandles.Lookup IMPL_LOOKUP = getImplLookup();

    private static MethodHandles.Lookup getImplLookup() {
        try {
            // Get theUnsafe
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Unsafe unsafe = (Unsafe) unsafeField.get(null);

            // Get IMPL_LOOKUP
            Field implLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            return (MethodHandles.Lookup) unsafe.getObject(unsafe.staticFieldBase(implLookupField), unsafe.staticFieldOffset(implLookupField));
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * add module-path at runtime
     */
    @SuppressWarnings("unchecked")
    public static void addModules(String modulePath) throws Throwable {
        // Find all extra modules
        ModuleFinder finder = ModuleFinder.of(Arrays.stream(modulePath.split(File.pathSeparator)).map(Paths::get).toArray(Path[]::new));
        MethodHandle loadModuleMH = IMPL_LOOKUP.findVirtual(Class.forName("jdk.internal.loader.BuiltinClassLoader"), "loadModule", MethodType.methodType(void.class, ModuleReference.class));

        // Resolve modules to a new config
        Configuration config = Configuration.resolveAndBind(finder, List.of(ModuleLayer.boot().configuration()), finder, finder.findAll().stream().peek(mref -> {
            try {
                // Load all extra modules in system class loader (unnamed modules for now)
                loadModuleMH.invokeWithArguments(ClassLoader.getSystemClassLoader(), mref);
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }).map(ModuleReference::descriptor).map(ModuleDescriptor::name).collect(Collectors.toList()));

        // Copy the new config graph to boot module layer config
        MethodHandle graphGetter = IMPL_LOOKUP.findGetter(Configuration.class, "graph", Map.class);
        HashMap<ResolvedModule, Set<ResolvedModule>> graphMap = new HashMap<>((Map<ResolvedModule, Set<ResolvedModule>>) graphGetter.invokeWithArguments(config));
        MethodHandle cfSetter = IMPL_LOOKUP.findSetter(ResolvedModule.class, "cf", Configuration.class);
        // Reset all extra resolved modules config to boot module layer config
        graphMap.forEach((k, v) -> {
            try {
                cfSetter.invokeWithArguments(k, ModuleLayer.boot().configuration());
                v.forEach(m -> {
                    try {
                        cfSetter.invokeWithArguments(m, ModuleLayer.boot().configuration());
                    } catch (Throwable throwable) {
                        throw new RuntimeException(throwable);
                    }
                });
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        });
        graphMap.putAll((Map<ResolvedModule, Set<ResolvedModule>>) graphGetter.invokeWithArguments(ModuleLayer.boot().configuration()));
        IMPL_LOOKUP.findSetter(Configuration.class, "graph", Map.class).invokeWithArguments(ModuleLayer.boot().configuration(), new HashMap<>(graphMap));

        // Reset boot module layer resolved modules as new config resolved modules to prepare define modules
        Set<ResolvedModule> oldBootModules = ModuleLayer.boot().configuration().modules();
        MethodHandle modulesSetter = IMPL_LOOKUP.findSetter(Configuration.class, "modules", Set.class);
        HashSet<ResolvedModule> modulesSet = new HashSet<>(config.modules());
        modulesSetter.invokeWithArguments(ModuleLayer.boot().configuration(), new HashSet<>(modulesSet));

        // Prepare to add all of the new config "nameToModule" to boot module layer config
        MethodHandle nameToModuleGetter = IMPL_LOOKUP.findGetter(Configuration.class, "nameToModule", Map.class);
        HashMap<String, ResolvedModule> nameToModuleMap = new HashMap<>((Map<String, ResolvedModule>) nameToModuleGetter.invokeWithArguments(ModuleLayer.boot().configuration()));
        nameToModuleMap.putAll((Map<String, ResolvedModule>) nameToModuleGetter.invokeWithArguments(config));
        IMPL_LOOKUP.findSetter(Configuration.class, "nameToModule", Map.class).invokeWithArguments(ModuleLayer.boot().configuration(), new HashMap<>(nameToModuleMap));

        // Define all extra modules and add all of the new config "nameToModule" to boot module layer config
        ((Map<String, Module>) IMPL_LOOKUP.findGetter(ModuleLayer.class, "nameToModule", Map.class).invokeWithArguments(ModuleLayer.boot())).putAll((Map<String, Module>) IMPL_LOOKUP.findStatic(Module.class, "defineModules", MethodType.methodType(Map.class, Configuration.class, Function.class, ModuleLayer.class)).invokeWithArguments(ModuleLayer.boot().configuration(), (Function<String, ClassLoader>) name -> ClassLoader.getSystemClassLoader(), ModuleLayer.boot()));

        // Add all of resolved modules
        modulesSet.addAll(oldBootModules);
        modulesSetter.invokeWithArguments(ModuleLayer.boot().configuration(), new HashSet<>(modulesSet));

        // Reset cache of boot module layer
        IMPL_LOOKUP.findSetter(ModuleLayer.class, "modules", Set.class).invokeWithArguments(ModuleLayer.boot(), null);
        IMPL_LOOKUP.findSetter(ModuleLayer.class, "servicesCatalog", Class.forName("jdk.internal.module.ServicesCatalog")).invokeWithArguments(ModuleLayer.boot(), null);

        // Add reads from extra modules to jdk modules
        MethodHandle implAddReadsMH = IMPL_LOOKUP.findVirtual(Module.class, "implAddReads", MethodType.methodType(void.class, Module.class));
        config.modules().forEach(rm -> ModuleLayer.boot().findModule(rm.name()).ifPresent(m -> oldBootModules.forEach(brm -> ModuleLayer.boot().findModule(brm.name()).ifPresent(bm -> {
            try {
                implAddReadsMH.invokeWithArguments(m, bm);
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }))));
    }

    public static void addExports(List<String> exports) throws Throwable {
        MethodHandle implAddExportsMH = IMPL_LOOKUP.findVirtual(Module.class, "implAddExports", MethodType.methodType(void.class, String.class, Module.class));
        MethodHandle implAddExportsToAllUnnamedMH = IMPL_LOOKUP.findVirtual(Module.class, "implAddExportsToAllUnnamed", MethodType.methodType(void.class, String.class));

        addExtra(exports, implAddExportsMH, implAddExportsToAllUnnamedMH);
    }

    public static void addOpens(List<String> opens) throws Throwable {
        MethodHandle implAddOpensMH = IMPL_LOOKUP.findVirtual(Module.class, "implAddOpens", MethodType.methodType(void.class, String.class, Module.class));
        MethodHandle implAddOpensToAllUnnamedMH = IMPL_LOOKUP.findVirtual(Module.class, "implAddOpensToAllUnnamed", MethodType.methodType(void.class, String.class));

        addExtra(opens, implAddOpensMH, implAddOpensToAllUnnamedMH);
    }

    private static void addExtra(List<String> extras, MethodHandle implAddExtraMH, MethodHandle implAddExtraToAllUnnamedMH) {
        extras.forEach(extra -> {
            ParserData data = parseModuleExtra(extra);
            if (data != null) {
                ModuleLayer.boot().findModule(data.module).ifPresent(m -> {
                    try {
                        if ("ALL-UNNAMED".equals(data.target)) {
                            implAddExtraToAllUnnamedMH.invokeWithArguments(m, data.packages);
                        } else {
                            ModuleLayer.boot().findModule(data.target).ifPresent(tm -> {
                                try {
                                    implAddExtraMH.invokeWithArguments(m, data.packages, tm);
                                } catch (Throwable t) {
                                    throw new RuntimeException(t);
                                }
                            });
                        }
                    } catch (Throwable t) {
                        throw new RuntimeException(t);
                    }
                });
            }
        });
    }

    // <module>/<package>=<target>
    private static ParserData parseModuleExtra(String extra) {
        String[] all = extra.split("=", 2);
        if (all.length < 2) {
            return null;
        }

        String[] source = all[0].split("/", 2);
        if (source.length < 2) {
            return null;
        }
        return new ParserData(source[0], source[1], all[1]);
    }

    private static class ParserData {
        final String module;
        final String packages;
        final String target;

        ParserData(String module, String packages, String target) {
            this.module = module;
            this.packages = packages;
            this.target = target;
        }
    }

    // ForgeWrapper need some extra settings to invoke BootstrapLauncher.
    public static void setupBootstrapLauncher() throws Throwable {
        MethodHandle implAddOpensMH = IMPL_LOOKUP.findVirtual(Module.class, "implAddOpens", MethodType.methodType(void.class, String.class, Module.class));
        ModuleLayer.boot().findModule("cpw.mods.bootstraplauncher").ifPresent(m -> {
            try {
                implAddOpensMH.invokeWithArguments(m, "cpw.mods.bootstraplauncher", ModuleUtil.class.getModule());
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        });
    }
}

package com.ruoyi.web;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Offline mapping introspection only: no application startup, database or production requests. */
class ControllerMappingInventoryAuditTest {
    static class MappingReader extends RequestMappingHandlerMapping {
        RequestMappingInfo mapping(Method method, Class<?> controller) {
            return getMappingForMethod(method, controller);
        }
    }

    @Test void writeCurrentControllerMappings(@TempDir Path tempDir) throws Exception {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));
        var reader = new MappingReader();
        reader.setEmbeddedValueResolver(value -> value);
        var routes = new ArrayList<Map<String, Object>>();
        var controllers = new ArrayList<String>();
        for (var candidate : scanner.findCandidateComponents("com.ruoyi")) {
            Class<?> type = Class.forName(candidate.getBeanClassName());
            controllers.add(type.getName());
            Map<Method, RequestMappingInfo> methods = MethodIntrospector.selectMethods(type,
                    (MethodIntrospector.MetadataLookup<RequestMappingInfo>) method -> reader.mapping(method, type));
            for (var entry : methods.entrySet()) {
                var info = entry.getValue();
                var method = entry.getKey();
                var row = new LinkedHashMap<String, Object>();
                row.put("controller", type.getName());
                row.put("method", method.getName());
                row.put("declaringClass", method.getDeclaringClass().getName());
                row.put("mapping", info.toString());
                row.put("paths", info.getPatternValues().stream().sorted().toList());
                row.put("httpMethods", info.getMethodsCondition().getMethods().stream().map(Enum::name).sorted().toList());
                var classPermission = type.getAnnotation(PreAuthorize.class);
                var methodPermission = method.getAnnotation(PreAuthorize.class);
                row.put("classPreAuthorize", classPermission == null ? null : classPermission.value());
                row.put("methodPreAuthorize", methodPermission == null ? null : methodPermission.value());
                routes.add(row);
            }
        }
        routes.sort(Comparator.comparing(row -> row.get("controller") + "." + row.get("method")));
        controllers.sort(String::compareTo);
        var result = new LinkedHashMap<String, Object>();
        result.put("kind", "offline Spring annotation introspection; not live registered mappings or semantic audit");
        result.put("controllers", controllers);
        result.put("routes", routes);
        String configuredOutput = System.getProperty("audit.mapping.output");
        Path output = configuredOutput == null ? tempDir.resolve("controller-mappings.json") : Path.of(configuredOutput);
        if (Files.exists(output)) throw new IllegalStateException("preserve existing inventory");
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(output.toFile(), result);
        System.out.println("Controller classes: " + controllers.size() + "; mapped methods: " + routes.size());
    }
}

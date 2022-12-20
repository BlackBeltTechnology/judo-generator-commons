package hu.blackbelt.judo.generator.commons;

import java.io.IOException;
import java.net.URL;

public interface URLResolver {
    URL getResource(String location) throws IOException;
}

package eu.gaiax.difs.fc.core.util;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import eu.gaiax.difs.fc.core.pojo.ContentAccessor;
import eu.gaiax.difs.fc.core.pojo.ContentAccessorFile;

public class TestUtil {

    public static ContentAccessor getAccessor(String path) throws UnsupportedEncodingException {
        return getAccessor(TestUtil.class, path);
    }
    
    public static ContentAccessor getAccessor(Class<?> testClass, String fileName) throws UnsupportedEncodingException {
        URL url = testClass.getClassLoader().getResource(fileName);
        String str = URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8.name());
        return new ContentAccessorFile(new File(str));
    }
    
}

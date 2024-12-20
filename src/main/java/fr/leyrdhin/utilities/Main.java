package fr.leyrdhin.utilities;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;

public class Main {
    public static void main(String[] args) throws Exception {
        File rootDir = new File(args[0]);
        for (File jpegFile : FileUtils.listFiles(rootDir, new SuffixFileFilter(new String[]{".jpg", ".jpeg"}, IOCase.INSENSITIVE), FalseFileFilter.INSTANCE)) {
            System.out.println(jpegFile.getAbsolutePath());
            Metadata metadata = ImageMetadataReader.readMetadata(jpegFile);
            System.out.println(metadata);

        }
    }
}
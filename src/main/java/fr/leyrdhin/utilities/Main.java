package fr.leyrdhin.utilities;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

public class Main {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");

    public static void main(String[] args) throws Exception {
        File rootDir = new File(args[0]);
        for (File jpegFile : FileUtils.listFiles(rootDir, new SuffixFileFilter(new String[]{".jpg", ".jpeg"}, IOCase.INSENSITIVE), TrueFileFilter.INSTANCE)) {
            System.out.println(jpegFile.getAbsolutePath());
            Metadata metadata = ImageMetadataReader.readMetadata(jpegFile);
            Collection<ExifSubIFDDirectory> exifDirectoryBase = metadata.getDirectoriesOfType(ExifSubIFDDirectory.class);
            for (ExifDirectoryBase directory : exifDirectoryBase) {
                Date date = directory.getDate(ExifDirectoryBase.TAG_DATETIME_DIGITIZED);
                if (date != null) {
                    String formattedDate = DATE_FORMAT.format(date);

                    File newFile = new File(jpegFile.getParent(), formattedDate + ".jpg");
                    FileUtils.moveFile(jpegFile, newFile);
                    break;
                }
            }
        }
    }
}
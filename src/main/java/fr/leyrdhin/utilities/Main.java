package fr.leyrdhin.utilities;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.TimeZone;

public class Main {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");
    private static final SimpleDateFormat VID_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

    static {
        VID_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    public static void main(String[] args) throws Exception {
        Options options = defineOptions();
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine parsedArgs = parser.parse(options, args);
            if (parsedArgs.getArgList().size() != 1) {
                printHelp(options);
            }
            File rootDir = new File(parsedArgs.getArgList().get(0));
            ParsedOptions parsedOptions = new ParsedOptions(parsedArgs);
            renamePhotos(rootDir, parsedOptions, new String[]{".jpg", ".jpeg"}, ".jpg");
            renamePhotos(rootDir, parsedOptions, new String[]{".arw"}, ".arw");
            processVideos(rootDir, parsedOptions);
        } catch (ParseException e) {
            printHelp(options);
        }
    }

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar image-renamer.jar <root directory>", options);
        System.exit(1);
    }

    private static Options defineOptions() {
        Options options = new Options();
        options.addOption("t", "timezone", true, "Timezone to use for date parsing. Default is Europe/Paris.");
        options.addOption("f", "ffmpeg-home", true, "Path to ffmpeg executables. Default is /usr/bin/.");
        return options;
    }

    private static class ParsedOptions {
        private final TimeZone timeZone;
        private final File ffmpegHome;

        ParsedOptions(CommandLine parsedArgs) {
            this.timeZone = TimeZone.getTimeZone(parsedArgs.hasOption('t') ? parsedArgs.getOptionValue('t') : "Europe/Paris");
            this.ffmpegHome = new File(parsedArgs.hasOption('f') ? parsedArgs.getOptionValue('f') : "/usr/bin");
        }
    }


    private static void renamePhotos(File rootDir, ParsedOptions parsedArgs, String[] sourceExtensions, String targetExtension) throws ImageProcessingException, IOException {
        int rootDirAbsPathLength = rootDir.getAbsolutePath().length();
        for (File jpegFile : FileUtils.listFiles(rootDir, new SuffixFileFilter(sourceExtensions, IOCase.INSENSITIVE), TrueFileFilter.INSTANCE)) {
            System.out.println(jpegFile.getAbsolutePath());
            Metadata metadata = ImageMetadataReader.readMetadata(jpegFile);
            Collection<ExifSubIFDDirectory> exifDirectoryBase = metadata.getDirectoriesOfType(ExifSubIFDDirectory.class);
            for (ExifDirectoryBase directory : exifDirectoryBase) {
//                Date date = directory.getDate(ExifDirectoryBase.TAG_DATETIME_DIGITIZED, parsedArgs.timeZone);
                Date date = directory.getDate(ExifDirectoryBase.TAG_DATETIME_DIGITIZED, parsedArgs.timeZone);
                if (date != null) {
                    date = correctWithTimezone(parsedArgs, directory, date);
                    File newFile = datedFile(jpegFile, date, targetExtension);
                    if (newFile == null) {
                        break;
                    }
                    System.out.println("Renaming " + jpegFile.getAbsolutePath().substring(rootDirAbsPathLength)
                            + " to " + newFile.getAbsolutePath().substring(rootDirAbsPathLength));
                    FileUtils.moveFile(jpegFile, newFile);
                    break;
                }
            }
        }
    }

    private static Date correctWithTimezone(ParsedOptions parsedArgs, ExifDirectoryBase directory, Date date) {
        String tzDigit = directory.getString(ExifDirectoryBase.TAG_TIME_ZONE_DIGITIZED);
        int rawOffset = parsedArgs.timeZone.getRawOffset();
        if (tzDigit != null) {
            String tzDigitWithoutMinutes = tzDigit.replaceFirst("^(.*):.*$", "$1");
            long tzOffset = Integer.parseInt(tzDigitWithoutMinutes) * 60 * 1000;
            date = new Date(date.getTime() + tzOffset - rawOffset);

        }
        return date;
    }

    private static File datedFile(File sourceFile, Date date, String extension) {
        String formattedDate = DATE_FORMAT.format(date);
        File newFile = new File(sourceFile.getParent(), formattedDate + extension);
        if (newFile.exists()) {
            if (newFile.equals(sourceFile)) {
                // no renaming to do --> skipping
                return null;
            }
            String newFileName = newFile.toString();
            String newFileNameWithoutExtension = newFileName.substring(0, newFileName.length() - extension.length());
            int i = 1;
            do {
                newFile = new File(newFileNameWithoutExtension + "_" + i++ + extension);
            } while (newFile.exists());
        }
        return newFile;
    }

    private static void processVideos(File rootDir, ParsedOptions parsedOptions) throws Exception {
        int rootDirAbsPathLength = rootDir.getAbsolutePath().length();
        for (File mp4File : FileUtils.listFiles(rootDir, new SuffixFileFilter(new String[]{".mp4", ".mpg4", ".mpeg4"}, IOCase.INSENSITIVE), TrueFileFilter.INSTANCE)) {
            renameVideoByDateAndReEncode(parsedOptions, mp4File, rootDirAbsPathLength);
        }
    }

    private static void renameVideoByDateAndReEncode(ParsedOptions parsedOptions, File mp4File, int rootDirAbsPathLength) throws Exception {
        FFprobe ffprobe = new FFprobe(new File(parsedOptions.ffmpegHome, "ffprobe").getAbsolutePath());
        FFmpegProbeResult probeResult = ffprobe.probe(mp4File.getAbsolutePath());

        if (!probeResult.getFormat().tags.containsKey("creation_time")) {
            System.err.println("No creation_time tag found in " + mp4File.getAbsolutePath() + ". Skipping");
            return;
        }
        Date creationTime = VID_DATE_FORMAT.parse(probeResult.getFormat().tags.get("creation_time"));
        File newFile = datedFile(mp4File, creationTime, ".mp4");
        if (newFile == null) {
            return;
        }
        System.out.println("Renaming " + mp4File.getAbsolutePath().substring(rootDirAbsPathLength)
                + " to " + newFile.getAbsolutePath().substring(rootDirAbsPathLength));
        FileUtils.moveFile(mp4File, newFile);
        reEncodeVideo(parsedOptions, newFile, probeResult.getStreams().get(0).width, probeResult.getStreams().get(0).height);
    }

    private static void reEncodeVideo(ParsedOptions parsedOptions, File newFile, int originalWidth, int originalHeight) throws Exception {
        FFmpeg ffmpeg = new FFmpeg(new File(parsedOptions.ffmpegHome, "ffmpeg").getAbsolutePath());
        FFprobe ffprobe = new FFprobe(new File(parsedOptions.ffmpegHome, "ffprobe").getAbsolutePath());

        String newFileName = newFile.getAbsolutePath();
        File tempOutputFile = new File(newFileName.substring(0, newFileName.length() - 4) + "_temp" + RANDOM.nextInt(0, 99) + ".mp4");
        int newWidth = originalWidth * 720 / originalHeight;

        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(newFile.getAbsolutePath())
                .overrideOutputFiles(true)
                .addOutput(tempOutputFile.getAbsolutePath())
                .disableSubtitle()       // No subtiles
                .setAudioChannels(1)         // Mono audio
                .setAudioCodec("aac")        // using the aac codec
                .setAudioSampleRate(48_000)  // at 48KHz
                .setAudioBitRate(32768)      // at 32 kbit/s
                .setVideoCodec("libx264")     // Video using x264
                .setVideoFrameRate(24, 1)     // at 24 frames per second
                .setVideoResolution(newWidth, 720) // with a height of 720p and keeping proportions
                .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL) // Allow FFmpeg to use experimental specs
                .setVideoMovFlags("use_metadata_tags") // copy metadata like capture date
                .addExtraArgs("-map_metadata", "0")
                .done();

        System.out.println("Reencoding " + newFile.getAbsolutePath() + " to " + tempOutputFile.getAbsolutePath() + " to X264 720p + AAC");

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        executor.createJob(builder).run();

        if (!newFile.delete()) {
            new RuntimeException("Could not delete " + newFile.getAbsolutePath());
        }
        FileUtils.moveFile(tempOutputFile, newFile);
    }
}
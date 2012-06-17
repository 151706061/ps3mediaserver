package net.pms.dlna;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import net.pms.PMS;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapperImpl;
import net.pms.util.ProcessUtil;

import org.slf4j.LoggerFactory;

/*
 * Handles thumbnail image of InputFile.
 * To create the image, on OSX uses internal app "sips", 
 * on other platforms uses ImageMagick.
 */
public class ThumbnailImage {

	private String absolutePath;
	private String name;
	private OutputParams params;
	private String outputFileName;

	public ThumbnailImage(InputFile media) throws IOException {
		absolutePath = media.getFile().getAbsolutePath();
		name = media.getFile().getName();
		params = setParams(media.getPush());
		outputFileName = PMS.getConfiguration().getTempFolder()
				+ "/imagemagick_thumbs/" + name + ".jpg";
	}

	/*
	 * Relies on OSX "sips"
	 */
	public void createThumbnailImageWithSips() throws IOException {
		String[] args = createSipsCommand();
		execute(args, params);
	}

	/*
	 * Relies on ImageMagick
	 */
	public void ceateThumbnailImageWithImageMagick() throws IOException {
		String[] args = createImageMagickCommand();
		execute(args, params);
	}

	public byte[] getThumbnailAsBytes() throws IOException {
		byte[] bytes = null;
		File genereatedThumbnail = new File(outputFileName);
		if (genereatedThumbnail.exists()) {
			bytes = readBytes(bytes, genereatedThumbnail);
		}
		return bytes;
	}

	public void deleteGeneratedImage() {
		File genereatedThumbnail = new File(outputFileName);
		if (genereatedThumbnail.exists()) {
			if (!genereatedThumbnail.delete()) {
				genereatedThumbnail.deleteOnExit();
			}
		}
	}

	private byte[] readBytes(byte[] bytes, File jpg) throws IOException {
		InputStream is = new FileInputStream(jpg);
		int sz = is.available();
		if (sz > 0) {
			bytes = new byte[sz];
			is.read(bytes);
		}
		is.close();
		return bytes;
	}

	private void execute(String[] args, OutputParams params) {
		final ProcessWrapperImpl pw = new ProcessWrapperImpl(args, params);
		pw.runInSameThread();
	}

	private OutputParams setParams(IPushOutput output) throws IOException {
		OutputParams params = new OutputParams(PMS.getConfiguration());
		params.workDir = new File(PMS.getConfiguration().getTempFolder()
				.getAbsolutePath()
				+ "/imagemagick_thumbs/");

		if (!params.workDir.exists() && !params.workDir.mkdirs()) {
			LoggerFactory.getLogger(PMS.class).debug(
					"Could not create directory \""
							+ params.workDir.getAbsolutePath() + "\"");
		}

		params.maxBufferSize = 1;
		params.stdin = output;
		params.log = true;
		params.noexitcheck = true; // not serious if anything happens during the
									// thumbnailer
		return params;
	}

	private String[] createSipsCommand() throws IOException {
		// sips -s format jpeg input.png --resampleHeightWidthMax 160 --out
		// output.jpg
		String args[] = new String[9];
		args[0] = "/usr/bin/sips";
		args[1] = "-s";
		args[2] = "format";
		args[3] = "jpeg";
		args[4] = ProcessUtil.getShortFileNameIfWideChars(absolutePath);
		args[5] = "--resampleHeightWidthMax";
		args[6] = "160";
		args[7] = "--out";
		args[8] = outputFileName;
		return args;
	}

	private String[] createImageMagickCommand() throws IOException {
		// convert -size 320x180 hatching_orig.jpg -auto-orient -thumbnail
		// 160x90 -unsharp 0x.5 thumbnail.gif
		String args[] = new String[10];
		args[0] = PMS.getConfiguration().getIMConvertPath();
		args[1] = "-size";
		args[2] = "320x180";
		args[3] = ProcessUtil.getShortFileNameIfWideChars(absolutePath);
		args[4] = "-auto-orient";
		args[5] = "-thumbnail";
		args[6] = "160x90";
		args[7] = "-unsharp";
		args[8] = "-0x.5";
		args[9] = outputFileName;
		return args;
	}
}

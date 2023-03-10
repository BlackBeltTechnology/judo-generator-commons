package hu.blackbelt.judo.generator.commons;

import lombok.Getter;
import lombok.Setter;

/**
 * It represents a generated file. It is used to write to directory or create a ZipStream.
 */
@Getter
@Setter
public class GeneratedFile {

	private String path;
	private byte[] content;

}

package co.com.gsdd.file.util.model.common;

import java.io.Serializable;

import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Generated
@Getter
@Setter
@ToString
public class UploadableFile implements Serializable {

	private static final long serialVersionUID = 4691510311799060770L;
	private String user;
    private String pass;
    private String encoding;

}

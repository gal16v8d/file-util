package com.gsdd.file.util.model;

import com.gsdd.file.util.model.common.UploadableFile;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Generated
@Getter
@Setter
@ToString
public class UploadableFtpFile extends UploadableFile {

  private static final long serialVersionUID = -4986146704253121853L;
  private String server;
  private int port;
  private boolean enableReply;
}

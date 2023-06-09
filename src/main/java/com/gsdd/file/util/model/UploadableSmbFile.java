package com.gsdd.file.util.model;

import com.gsdd.file.util.model.common.UploadableFile;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Generated
@Getter
@Setter
@ToString
public class UploadableSmbFile extends UploadableFile {

  private static final long serialVersionUID = 3616139956403881273L;
  private String url;
  private SmbFile route;
  private boolean reconnect;
  private NtlmPasswordAuthentication auth;
}

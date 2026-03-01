package it.water.documents.manager.repository.s3.config;


public class DocumentRepositoryS3Constant {

    private DocumentRepositoryS3Constant() {}


    public static final String PROP_S3_ENDPOINT = "it.water.storage.endpoint";
    public static final String PROP_S3_ACCESS_KEY = "it.water.storage.access-key";
    public static final String PROP_S3_SECRET_KEY = "it.water.storage.secret-key";
    public static final String PROP_S3_REGION = "it.water.storage.region";
    public static final String PROP_S3_BUCKET = "it.water.storage.bucket";
    public static final String PROP_S3_PATH_STYLE = "it.water.storage.path-style-enabled";

    // DEFAULTS VALUES
    public static final String DEFAULT_STORAGE_ENDPOINT = "http://localhost:9000";
    public  static final String DEFAULT_STORAGE_KEY = "";
    public static final String DEFAULT_BUCKET = "bucket";
    public  static final String DEFAULT_REGION = "us-east-1";
    public static final boolean DEFAULT_PATH_STYLE_ENABLED = false;

    // S3 OPERATION NAMES
    public static final String OP_UPLOAD = "upload";
    public static final String OP_STREAM_UPLOAD = "stream upload";
    public static final String OP_DOWNLOAD = "download";
    public static final String OP_STREAM_DOWNLOAD = "stream download";
    public static final String OP_DELETE = "delete";
    public static final String OP_COPY = "copy";

    // S3 ERROR MESSAGES
    public static final String ERR_FILE_NOT_FOUND = "S3 %s failed: file not found (bucket=%s, key=%s)";
    public static final String ERR_BUCKET_NOT_FOUND = "S3 bucket '%s' does not exist at endpoint '%s'";
    public static final String ERR_CONNECTION = "Cannot connect to S3 at '%s' during %s (bucket=%s, key=%s)";
    public static final String ERR_S3 = "S3 error during %s (bucket=%s, key=%s, status=%d)";
    public static final String ERR_UNEXPECTED = "Unexpected error during S3 %s (bucket=%s, key=%s)";
}

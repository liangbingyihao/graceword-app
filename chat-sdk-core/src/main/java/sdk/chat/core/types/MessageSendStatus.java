package sdk.chat.core.types;

/**
 * Created by ben on 9/29/17.
 */

public enum MessageSendStatus {
    Initial,
    //    Compressing,
    Uploading,
    UploadFailed,
    Replying,
    //    DidUpload,
//    WillSend,
    Failed,
    Sent,
    None,
//    Incoming,
}

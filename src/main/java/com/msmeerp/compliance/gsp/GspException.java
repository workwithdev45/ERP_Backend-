package com.msmeerp.compliance.gsp;

import com.msmeerp.common.exception.BadRequestException;

import java.util.List;

/** A rejection from the IRP / e-way bill system, carrying its error codes and messages. */
public class GspException extends BadRequestException {

    public GspException(List<String> errors) {
        super(String.join("; ", errors));
    }

    public GspException(String error) {
        super(error);
    }
}

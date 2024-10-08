/*
 * Copyright 2023 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.linecorp.armeria.common.grpc;

import static java.util.Objects.requireNonNull;

import com.linecorp.armeria.common.RequestContext;
import com.linecorp.armeria.common.annotation.Nullable;
import com.linecorp.armeria.common.annotation.UnstableApi;

import io.grpc.Metadata;
import io.grpc.Status;

/**
 * An interface that converts a {@link Throwable} into a gRPC {@link Status} for gRPC exception handler.
 */
@UnstableApi
@FunctionalInterface
public interface GrpcExceptionHandlerFunction {

    /**
     * Returns a newly created {@link GrpcExceptionHandlerFunctionBuilder}.
     */
    static GrpcExceptionHandlerFunctionBuilder builder() {
        return new GrpcExceptionHandlerFunctionBuilder();
    }

    /**
     * Returns the default {@link GrpcExceptionHandlerFunction}.
     */
    @UnstableApi
    static GrpcExceptionHandlerFunction of() {
        return DefaultGrpcExceptionHandlerFunction.INSTANCE;
    }

    /**
     * Maps the specified {@link Throwable} to a gRPC {@link Status} and mutates the specified {@link Metadata}.
     * If {@code null} is returned, {@link #of()} will be used to return {@link Status} as the default.
     *
     * <p>The specified {@link Status} parameter was created via {@link Status#fromThrowable(Throwable)}.
     * You can return the {@link Status} or any other {@link Status} as needed.
     */
    @Nullable
    Status apply(RequestContext ctx, Status status, Throwable cause, Metadata metadata);

    /**
     * Returns a {@link GrpcExceptionHandlerFunction} that returns the result of this function
     * when this function returns non {@code null} result, in which case the specified function isn't executed.
     * when this function returns {@code null}, returns a {@link GrpcExceptionHandlerFunction} that the result
     * of the specified {@link GrpcExceptionHandlerFunction}.
     */
    default GrpcExceptionHandlerFunction orElse(GrpcExceptionHandlerFunction next) {
        requireNonNull(next, "next");
        if (this == next) {
            return this;
        }
        return (ctx, status, cause, metadata) -> {
            final Status newStatus = apply(ctx, status, cause, metadata);
            if (newStatus != null) {
                return newStatus;
            }
            return next.apply(ctx, status, cause, metadata);
        };
    }
}

/**
 * Copyright 2012 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.provis.parser.model;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import com.google.common.base.Objects;

public class ListType
        extends ContainerType
{
    private final ThriftType type;

    public ListType(ThriftType type, String cppType, List<TypeAnnotation> annotations)
    {
        super(cppType, annotations);
        this.type = checkNotNull(type, "type");
    }

    public ThriftType getType()
    {
        return type;
    }

    @Override
    public String toString()
    {
        return Objects.toStringHelper(this)
                .add("type", type)
                .add("cppType", cppType)
                .add("annotations", annotations)
                .toString();
    }
}

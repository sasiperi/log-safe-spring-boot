package com.e2e.logging.adapter;
/*

import java.io.IOException;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.rest.webmvc.RootResourceInformation;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class RootResourceInformationAdapter extends TypeAdapter<RootResourceInformation>
{

    @Override
    public void write(JsonWriter out, RootResourceInformation value) throws IOException
    {
        out.beginObject();
        out.name("DomainType:");
        out.value(value.getResourceMetadata().getDomainType().getName());
        PersistentEntity<?, ?> entity = value.getPersistentEntity();
    
        out.name();
        out.value(entity.getName());

        out.name();
        out.value(entity.get());
        
        out.name();
        out.value();
        
        out.name();
        out.value();
        
        out.name();
        out.value();
        
        out.name();
        out.value();
        
        out.name();
        out.value();
    }

    @Override
    public RootResourceInformation read(JsonReader in) throws IOException
    {
        // TODO Auto-generated method stub
        return null;
    }

}
*/
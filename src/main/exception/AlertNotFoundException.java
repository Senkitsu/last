package com.needed.task.exception;

public class AlertNotFoundException extends RuntimeException{
    public AlertNotFoundException(Long id)
    {
        super("Incident with ID "+id+" is not found");
    }
}
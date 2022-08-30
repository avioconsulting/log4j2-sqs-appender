package com.avioconsulting.log4j.sqs.processor;

public class ProcessorAttributes {

 private String message;
 private String queueUrl ;
 private Integer maxMessageSize;

 public ProcessorAttributes(String message, String queueUrl, Integer maxMessageSize) {
  this.message = message;
  this.queueUrl = queueUrl;
  this.maxMessageSize = maxMessageSize;
 }

 public String getMessage() {
  return message;
 }

 public String getQueueUrl() {
  return queueUrl;
 }

 public Integer getMaxMessageSize() {
  return maxMessageSize;
 }

}

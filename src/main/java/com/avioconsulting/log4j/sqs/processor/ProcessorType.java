package com.avioconsulting.log4j.sqs.processor;

public enum ProcessorType {
		TRUNCATE,
		FIFO,
		S3,
		DISCARD,
		DEFAULT,
		EXTENDED
}

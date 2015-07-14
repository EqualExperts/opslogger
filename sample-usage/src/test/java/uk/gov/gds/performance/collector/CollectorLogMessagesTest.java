package uk.gov.gds.performance.collector;

import com.equalexperts.logging.LogMessageContractTest;

import static com.equalexperts.logging.EnumContractRunner.EnumToTest;

@EnumToTest(value= CollectorLogMessages.class)
public class CollectorLogMessagesTest extends LogMessageContractTest<CollectorLogMessages> {
}
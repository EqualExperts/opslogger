package uk.gov.gds.performance.collector;

import uk.gov.gds.performance.collector.logging.LogMessageContractTest;

import static uk.gov.gds.performance.collector.logging.EnumContractRunner.EnumToTest;

@EnumToTest(value= CollectorLogMessage.class)
public class CollectorLogMessageTest extends LogMessageContractTest<CollectorLogMessage> {
}
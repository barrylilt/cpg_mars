/**
    Copyright 2014-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.

    Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with the License. A copy of the License is located at

        http://aws.amazon.com/apache2.0/

    or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package cpg;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.Speechlet;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.Card;
import com.amazon.speech.ui.OutputSpeech;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;
import com.amazon.speech.ui.SsmlOutputSpeech;

public class CpgSpeechlet implements Speechlet {
	private static final Logger log = LoggerFactory.getLogger(CpgSpeechlet.class);

	/**
	 * The slots defined in Intent.
	 */

	private static final String SLOT_YEAR = "YEARNO";

	private static final String SLOT_BRAND = "BRAND";

	private static final String SLOT_CUSTOMER = "CUSTOMER";

	private ConnectionUtil connUtil;

	@Override
	public void onSessionStarted(final SessionStartedRequest request, final Session session) throws SpeechletException {
		log.info("onSessionStarted requestId={}, sessionId={}", request.getRequestId(), session.getSessionId());

		initializeComponents();
	}

	private void initializeComponents() {
		if (connUtil == null) {
			connUtil = new ConnectionUtil();
		}
	}

	@Override
	public SpeechletResponse onLaunch(final LaunchRequest request, final Session session) throws SpeechletException {
		log.info("onLaunch requestId={}, sessionId={}", request.getRequestId(), session.getSessionId());

		String speechOutput = "Hello there! Welcome to the CPG Analytics. How can i help you?";

		String repromptText = "Do you want me to wait! Please say yes or no!";

		// Here we are prompting the user for input
		return newAskResponse(speechOutput, false, "<speak>" + repromptText + "</speak>", true);
	}

	@Override
	public SpeechletResponse onIntent(final IntentRequest request, final Session session) throws SpeechletException {
		log.info("onIntent requestId={}, sessionId={}", request.getRequestId(), session.getSessionId());

		initializeComponents();
		
		Intent intent = request.getIntent();
		String intentName = (intent != null) ? intent.getName() : null;

		if ("TotalTradeSpend".equals(intentName)) {
			return getTotalTradeSpendResponse(intent, session);
		} else if ("TotalGrossProfit".equals(intentName)) {
			return getTotalGrossProfitResponse(intent, session);
		} else if ("TotalROI".equals(intentName)) {
			return getTotalROIResponse(intent, session);
		} else if ("BestPromotions".equals(intentName)) {
			return getBestPromotionsResponse(intent, session);
		} else if ("PromotionsPerformance".equals(intentName)) {
			return getPerfomanceResponse(intent, session);
		} else if ("HearMore".equals(intentName)) {
			return getMoreHelp();
		} else if ("DontHearMore".equals(intentName)) {
			PlainTextOutputSpeech output = new PlainTextOutputSpeech();
			output.setText("Thanks,  Please do come again....");
			return SpeechletResponse.newTellResponse(output);
		} else if ("AMAZON.HelpIntent".equals(intentName)) {
			return getHelp();
		} else if ("AMAZON.StopIntent".equals(intentName)) {
			PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
			outputSpeech.setText("Bye,  Hope to see you soon!");
			// connUtil.closeConnection();
			return SpeechletResponse.newTellResponse(outputSpeech);
		} else if ("AMAZON.CancelIntent".equals(intentName)) {
			PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
			outputSpeech.setText("Goodbye! ");
			// connUtil.closeConnection();
			return SpeechletResponse.newTellResponse(outputSpeech);
		} else {
			// Reprompt the user.
			String speechOutput = "I'm sorry I didn't understand that. Please try again, intent " + intentName;

			String repromptText = "I'm sorry I didn't understand that. You can ask things like, "
					+ "What are the top 5 promotions during year 2016 <break time=\"0.2s\" /> "
					+ "bottom 5 promotions during year 2016 <break time=\"0.2s\" /> "
					+ "What is Total ROI for Year 2016 <break time=\"0.2s\" /> "
					+ "Which Promo Mechanics are Working Best <break time=\"0.2s\" /> "
					+ "What is Total Gross Profit for Year 2016	";

			return newAskResponse(speechOutput, false, "<speak>" + repromptText + "</speak>", true);

		}
	}

	@Override
	public void onSessionEnded(final SessionEndedRequest request, final Session session) throws SpeechletException {
		log.info("onSessionEnded requestId={}, sessionId={}", request.getRequestId(), session.getSessionId());

	}

	/**
	 * Creates Dynamic Query for Total Trade Spend based on year
	 * 
	 * @param year
	 * 
	 * @return String totalTradeSpend
	 * 
	 */
	private String executeTotalTradeSpendQuery(String year) {

		// base Query
		String totalTradeSpendQuery = "select CONVERT(decimal(15), sum(TotalTradeSpendSellOut)) "
				+ "as 'Total Trade Spend' , datepart(yyyy,[PromotionStartDate])  from  "
				+ "[dbo].[VIEW_TABLEAU_PEA_DEMO] ";

		// if year is not empty add year in where condition
		if (StringUtils.isNotEmpty(year)) {
			totalTradeSpendQuery += "where datepart(yyyy,[PromotionStartDate])='2016' "
					+ "group by datepart(yyyy,[PromotionStartDate])";
		}

		// execute the query on server connection
		String totalTradeSpend = connUtil.executeQuery(totalTradeSpendQuery);

		return totalTradeSpend;

	}

	/**
	 * Creates Dynamic Query for Total Gross Profit based on brand
	 * 
	 * @param brand
	 * 
	 * @return String totalTradeSpend
	 * 
	 */

	private String executeTotalGrossProfitQuery(String brand) {

		// base Query
		String totalGrossProfitQuery = "select CONVERT(decimal(15), sum(GrossProfitSellOut)) as "
				+ "'Total Gross Profit', ProdLevel4Name as 'Brand' from  [dbo].[VIEW_TABLEAU_PEA_DEMO] ";

		// if brand is not empty add brand in where condition
		if (StringUtils.isNotEmpty(brand)) {
			totalGrossProfitQuery += "where ProdLevel4Name ='" + brand + "' group by ProdLevel4Name";
		}

		String totalGrossProfit = connUtil.executeQuery(totalGrossProfitQuery);

		return totalGrossProfit;

	}

	/**
	 * Creates Dynamic Query for Total ROI based on customer
	 * 
	 * @param customer
	 * 
	 * @return String totalTradeSpend
	 * 
	 */
	private String executeTotalROIQuery(String customer) {

		// base Query
		String totalROIQuery = "select CONVERT(decimal(4,1), sum(GrossProfitSellOut)/sum(TotalTradeSpendSellOut) * 100) "
				+ "as 'Total ROI', CustCustomerName as 'Customer' from  [dbo].[VIEW_TABLEAU_PEA_DEMO] ";

		// if customer is not empty add customer in where condition
		if (StringUtils.isNotEmpty(customer)) {
			totalROIQuery += "where CustCustomerName ='" + customer + "' group by CustCustomerName";
		}

		System.out.println("Total ROI Query :: " + totalROIQuery);

		String totalROI = connUtil.executeQuery(totalROIQuery);

		return totalROI;

	}

	/**
	 * Creates a Dynamic Query for Total Trade Spend based on the
	 * year/brand/customer, executes the Query and returns the result.
	 *
	 * @param intent
	 *            the intent for the request
	 * @param session
	 * @return SpeechletResponse spoken and visual response for the given intent
	 * @throws SpeechletException
	 * 
	 */
	private SpeechletResponse getTotalTradeSpendResponse(final Intent intent, final Session session)
			throws SpeechletException {

		// Simple Display Card
		SimpleCard card = new SimpleCard();
		card.setTitle("Total Trade Spend ::");

		String yearNo = intent.getSlot(SLOT_YEAR) != null ? intent.getSlot(SLOT_YEAR).getValue() : "";

		String brand = intent.getSlot(SLOT_BRAND) != null ? intent.getSlot(SLOT_BRAND).getValue() : "";

		String customer = intent.getSlot(SLOT_CUSTOMER) != null ? intent.getSlot(SLOT_CUSTOMER).getValue() : "";

		String answer = "The Total Trade Spend for ";

		String speechOut = "";

		String finalSpeechOut = "";

		String cardOut = "";

		String finalCardOut = "";

		if (StringUtils.isNotEmpty(yearNo)) {
			speechOut += "year <say-as interpret-as=\"cardinal\">" + yearNo + "</say-as>";
			cardOut += " year " + yearNo;
		}

		if (StringUtils.isNotEmpty(brand)) {
			speechOut += " brand " + brand;
			cardOut += " brand " + brand;
		}

		if (StringUtils.isNotEmpty(customer)) {
			speechOut += " customer " + customer;
			cardOut += " customer " + customer;
		}

		String totalTradeSpend = executeTotalTradeSpendQuery(yearNo); // "203090661"; 

		finalSpeechOut = answer + speechOut + " is <break time=\"0.2s\" /> <say-as interpret-as=\"cardinal\"> "
				+ totalTradeSpend + "</say-as>";

		finalCardOut = answer + cardOut + " is " + totalTradeSpend;

		card.setContent(finalCardOut);

		String repromptText = " Would you like to hear more ? Please say yes or no";

		return newAskResponse("<speak>" + finalSpeechOut + "</speak>", true, "<speak>" + repromptText + "</speak>",
				true, card);
	}

	/**
	 * Creates a Dynamic Query for Total Gross Profit based on the
	 * year/brand/customer, executes the Query and returns the result
	 *
	 * @param intent
	 *            the intent for the request
	 * @param session
	 * @return SpeechletResponse spoken and visual response for the given intent
	 * @throws SpeechletException
	 * 
	 */
	private SpeechletResponse getTotalGrossProfitResponse(final Intent intent, final Session session)
			throws SpeechletException {

		// Simple Display Card
		SimpleCard card = new SimpleCard();
		card.setTitle("Total Gross Profit :: ");

		String yearNo = intent.getSlot(SLOT_YEAR) != null ? intent.getSlot(SLOT_YEAR).getValue() : "";

		String brand = intent.getSlot(SLOT_BRAND) != null ? intent.getSlot(SLOT_BRAND).getValue() : "";

		String customer = intent.getSlot(SLOT_CUSTOMER) != null ? intent.getSlot(SLOT_CUSTOMER).getValue() : "";

		String answer = "The Total Gross Profit for ";

		String speechOut = "";

		String finalSpeechOut = "";

		String cardOut = "";

		String finalCardOut = "";

		if (StringUtils.isNotEmpty(yearNo)) {
			speechOut += "year <say-as interpret-as=\"cardinal\">" + yearNo + "</say-as>";
			cardOut += " year " + yearNo + " /n";
		}

		if (StringUtils.isNotEmpty(brand)) {
			speechOut += " brand " + brand;
			cardOut += " brand " + brand;
		}

		if (StringUtils.isNotEmpty(customer)) {
			speechOut += " customer " + customer;
			cardOut += " customer " + customer;
		}

		String totalGrossProfit = executeTotalGrossProfitQuery(brand); // "2376586"; 

		finalSpeechOut = answer + speechOut + " is <break time=\"0.2s\" /> <say-as interpret-as=\"cardinal\"> "
				+ totalGrossProfit + "</say-as>";

		finalCardOut = answer + cardOut + " is " + totalGrossProfit;

		card.setContent(finalCardOut);

		String repromptText = " Would you like to hear more ? Please say yes or no";

		return newAskResponse("<speak>" + finalSpeechOut + "</speak>", true, "<speak>" + repromptText + "</speak>",
				true, card);
	}

	/**
	 * Creates a Dynamic Query for Total ROI based on the year/brand/customer,
	 * executes the Query and returns the result.
	 *
	 * @param intent
	 *            the intent for the request
	 * @param session
	 * @return SpeechletResponse spoken and visual response for the given intent
	 * @throws SpeechletException
	 * 
	 */
	private SpeechletResponse getTotalROIResponse(final Intent intent, final Session session)
			throws SpeechletException {

		// Simple Display Card
		SimpleCard card = new SimpleCard();
		card.setTitle("Total Gross Profit :: ");

		String yearNo = intent.getSlot(SLOT_YEAR) != null ? intent.getSlot(SLOT_YEAR).getValue() : "";

		String brand = intent.getSlot(SLOT_BRAND) != null ? intent.getSlot(SLOT_BRAND).getValue() : "";

		String customer = intent.getSlot(SLOT_CUSTOMER) != null ? intent.getSlot(SLOT_CUSTOMER).getValue() : "";

		String answer = "The Total ROI for ";

		String speechOut = "";

		String finalSpeechOut = "";

		String cardOut = "";

		String finalCardOut = "";

		if (StringUtils.isNotEmpty(yearNo)) {
			speechOut += "year <say-as interpret-as=\"cardinal\">" + yearNo + "</say-as>";
			cardOut += " year " + yearNo + " /n";
		}

		if (StringUtils.isNotEmpty(brand)) {
			speechOut += " brand " + brand;
			cardOut += " brand " + brand;
		}

		if (StringUtils.isNotEmpty(customer)) {
			speechOut += " customer " + customer;
			cardOut += " customer " + customer;
		}

		String totalROI = executeTotalROIQuery(customer); // "260.3"; 

		finalSpeechOut = answer + speechOut + " is <break time=\"0.2s\" /> <say-as interpret-as=\"cardinal\"> "
				+ totalROI + "</say-as>";

		finalCardOut = answer + cardOut + " is " + totalROI;

		card.setContent(finalCardOut);

		String repromptText = " Would you like to hear more ? Please say yes or no";

		return newAskResponse("<speak>" + finalSpeechOut + "</speak>", true, "<speak>" + repromptText + "</speak>",
				true, card);
	}

	private SpeechletResponse getBestPromotionsResponse(final Intent intent, final Session session)
			throws SpeechletException {

		// Simple Display Card
		SimpleCard card = new SimpleCard();
		card.setTitle(":: Best Promotion for year 2016 ::");

		String answer = "<say-as interpret-as=\"digits\"> 316669  </say-as> KANTONG PINEAAPLE PRICE REDUCTION ";

		String cardOut = "316669  KANTONG PINEAAPLE PRICE REDUCTION ";

		card.setContent(cardOut);

		String repromptText = " Would you like to hear more ? Please say yes or no";

		return newAskResponse("<speak>" + answer + "</speak>", true, "<speak>" + repromptText + "</speak>", true, card);
	}

	private SpeechletResponse getPerfomanceResponse(final Intent intent, final Session session)
			throws SpeechletException {

		String speechOutput = " <say-as interpret-as=\"cardinal\"> 574 </say-as> out of "
				+ "<say-as interpret-as=\"cardinal\"> 3450 </say-as> Promotions did not perform well";

		String cardOut = "574 out of 3450 Promotions did not performed well.";

		// Create the Simple card content.
		SimpleCard card = new SimpleCard();

		card.setTitle(":: Promotions Performance for year 2016 ::");

		card.setContent(cardOut);

		String repromptText = " Would you like to hear more ? Please say yes or no";

		return newAskResponse("<speak>" + speechOutput + "</speak>", true, repromptText, false, card);
	}

	/**
	 * Instructs the user on how to interact with this skill.
	 */
	private SpeechletResponse getHelp() {

		String speechOutput = "You can ask for the things like following <break time=\"0.2s\" />"
				+ "What is the Total Trade Spend for Year 2016 <break time=\"0.2s\" />"
				+ "What is the Total ROI for Target, <break time=\"0.2s\" />"
				+ "What is the Total Gross Profit for MUSK <break time=\"0.2s\" /> " + " or you can say simply exit. "
				+ "Now, what can I help you with?";

		String repromptText = "I'm sorry I didn't understand that. You can ask things like,"
				+ "What is the Total Gross Profit for Neon <break time=\"0.2s\" /> "
				+ " Or you can say exit. Now, what can I help you with?";

		return newAskResponse("<speak>" + speechOutput + "</speak>", true, "<speak>" + repromptText + "</speak>", true);
	}

	/**
	 * Provides more help on how to interact with this skill.
	 */
	private SpeechletResponse getMoreHelp() throws SpeechletException {

		String speechOutput = "Waiting for your query!";

		String repromptText = "Here are few more samples, "
				+ "What are the top 5 Brands during year 2016 <break time=\"0.2s\" /> "
				+ "give me bottom 3  Products during year 2016 <break time=\"0.2s\" /> "
				+ "Tell me top 3 promotions during year 2016 <break time=\"0.2s\" /> "
				+ "Give me key time contributors for patient enrollment delays";

		// Here we are prompting the user for input
		return newAskResponse(speechOutput, false, "<speak>" + repromptText + "</speak>", true);
	}

	/**
	 * Wrapper for creating the Ask response from the input strings.
	 * 
	 * @param stringOutput
	 *            the output to be spoken
	 * @param isOutputSsml
	 *            whether the output text is of type SSML
	 * @param repromptText
	 *            the reprompt for if the user doesn't reply or is
	 *            misunderstood.
	 * @param isRepromptSsml
	 *            whether the reprompt text is of type SSML
	 * @param displayCard
	 *            the display text to be sent to device
	 * @return SpeechletResponse the speechlet response
	 */
	private SpeechletResponse newAskResponse(String stringOutput, boolean isOutputSsml, String repromptText,
			boolean isRepromptSsml, Card displayCard) {
		OutputSpeech outputSpeech, repromptOutputSpeech;
		if (isOutputSsml) {
			outputSpeech = new SsmlOutputSpeech();
			((SsmlOutputSpeech) outputSpeech).setSsml(stringOutput);
		} else {
			outputSpeech = new PlainTextOutputSpeech();
			((PlainTextOutputSpeech) outputSpeech).setText(stringOutput);
		}

		if (isRepromptSsml) {
			repromptOutputSpeech = new SsmlOutputSpeech();
			((SsmlOutputSpeech) repromptOutputSpeech).setSsml(repromptText);
		} else {
			repromptOutputSpeech = new PlainTextOutputSpeech();
			((PlainTextOutputSpeech) repromptOutputSpeech).setText(repromptText);
		}
		Reprompt reprompt = new Reprompt();
		reprompt.setOutputSpeech(repromptOutputSpeech);
		return SpeechletResponse.newAskResponse(outputSpeech, reprompt, displayCard);
	}

	/**
	 * Wrapper for creating the Ask response from the input strings.
	 * 
	 * @param stringOutput
	 *            the output to be spoken
	 * @param isOutputSsml
	 *            whether the output text is of type SSML
	 * @param repromptText
	 *            the reprompt for if the user doesn't reply or is
	 *            misunderstood.
	 * @param isRepromptSsml
	 *            whether the reprompt text is of type SSML
	 * @return SpeechletResponse the speechlet response
	 */
	private SpeechletResponse newAskResponse(String stringOutput, boolean isOutputSsml, String repromptText,
			boolean isRepromptSsml) {
		OutputSpeech outputSpeech, repromptOutputSpeech;
		if (isOutputSsml) {
			outputSpeech = new SsmlOutputSpeech();
			((SsmlOutputSpeech) outputSpeech).setSsml(stringOutput);
		} else {
			outputSpeech = new PlainTextOutputSpeech();
			((PlainTextOutputSpeech) outputSpeech).setText(stringOutput);
		}

		if (isRepromptSsml) {
			repromptOutputSpeech = new SsmlOutputSpeech();
			((SsmlOutputSpeech) repromptOutputSpeech).setSsml(repromptText);
		} else {
			repromptOutputSpeech = new PlainTextOutputSpeech();
			((PlainTextOutputSpeech) repromptOutputSpeech).setText(repromptText);
		}
		Reprompt reprompt = new Reprompt();
		reprompt.setOutputSpeech(repromptOutputSpeech);
		return SpeechletResponse.newAskResponse(outputSpeech, reprompt);
	}
}

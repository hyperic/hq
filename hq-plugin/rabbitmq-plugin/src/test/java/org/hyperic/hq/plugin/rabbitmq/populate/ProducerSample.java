/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.plugin.rabbitmq.populate;
 
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * PopulateData is purely to populate RabbitMQ QA servers.
 * Most of this is from Spring AMQP sample stock app, modified
 * for this simple purpose without having to include the entire jar.
 * @author Helena Edelson
 * @author Mark Pollack
 * @author Mark Fisher
 */
public class ProducerSample {

    private int numMessages;

    private static final Random random = new Random();

    private static final List<MockStock> stocks = new ArrayList<MockStock>();

    private RabbitTemplate rabbitTemplate;

    public ProducerSample(RabbitTemplate rabbitTemplate, int numMessages) {
        this.rabbitTemplate = rabbitTemplate;
        this.numMessages = numMessages;

        stocks.add(new MockStock("AAPL", StockExchange.nasdaq, 255));
        stocks.add(new MockStock("CSCO", StockExchange.nasdaq, 22));
        stocks.add(new MockStock("DELL", StockExchange.nasdaq, 15));
        stocks.add(new MockStock("GOOG", StockExchange.nasdaq, 500));
        stocks.add(new MockStock("INTC", StockExchange.nasdaq, 22));
        stocks.add(new MockStock("MSFT", StockExchange.nasdaq, 29));
        stocks.add(new MockStock("ORCL", StockExchange.nasdaq, 24));
        stocks.add(new MockStock("CAJ", StockExchange.nyse, 43));
        stocks.add(new MockStock("F", StockExchange.nyse, 12));
        stocks.add(new MockStock("GE", StockExchange.nyse, 18));
        stocks.add(new MockStock("HMC", StockExchange.nyse, 32));
        stocks.add(new MockStock("HPQ", StockExchange.nyse, 48));
        stocks.add(new MockStock("IBM", StockExchange.nyse, 130));
        stocks.add(new MockStock("TM", StockExchange.nyse, 76));
    }

    public void sendMessages() {
        for (int i = 1; i <= numMessages; i++) {
            sendMarketData();
            /*try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                System.out.println(e);
            }*/ 
        }

        System.out.println("Sending complete");
    }

    protected void send(TradeRequest tradeRequest) {
        rabbitTemplate.convertAndSend(tradeRequest, new MessagePostProcessor() {
            public Message postProcessMessage(Message message) throws AmqpException {
                message.getMessageProperties().setReplyTo(new Address("queuename")); //todo
                message.getMessageProperties().setCorrelationId(Thread.currentThread().getName().getBytes());
                return message;
            }
        });
    }

    private void sendMarketData() {
        Quote quote = generateFakeQuote();
        Stock stock = quote.getStock();
        System.out.println("Sending Market Data for " + stock.getTicker());
        String routingKey = "stocks.quotes";
        /** Until I set up a converter for from byte[] */ 
        rabbitTemplate.convertAndSend(quote.toString());
    }

    private Quote generateFakeQuote() {
        MockStock stock = stocks.get(random.nextInt(stocks.size()));
        String price = stock.randomPrice();
        return new Quote(stock, price);
    }
 
    private class MockStock extends Stock {

        private final int basePrice;
        private final DecimalFormat twoPlacesFormat = new DecimalFormat("0.00");

        private MockStock(String ticker, StockExchange stockExchange, int basePrice) {
            this.setTicker(ticker);
            this.setStockExchange(stockExchange);
            this.basePrice = basePrice;
        }

        private String randomPrice() {
            return this.twoPlacesFormat.format(this.basePrice + Math.abs(random.nextGaussian()));
        }
    }
 
}

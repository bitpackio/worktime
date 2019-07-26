/*
 *   Worktime - transform ical calendars to xml events
 * 
 *   Copyright (C) 2014 bitpack.io <hello@bitpack.io>
 * 
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 * 
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License at <http://www.gnu.org/licenses/> for
 *   more details.
 * 
 */

import java.io.FileOutputStream;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.Iterator;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import net.fortuna.ical4j.data.*;
import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.filter.*;

public final class Worktime {

    private final static boolean VERBOSE = true;

    private final static String[] PROPERTIES = new String[] {
        "START", "DTSTART", "END", "DTEND", "LOCATION", "AUTHOR", "SUMMARY", "DESCRIPTION"
    };
	
    private final static String FORMAT = "yyyyMMdd'T'HHmmss";
    private final static SimpleDateFormat SDF = new SimpleDateFormat(FORMAT);
    private final static java.util.Calendar CAL = java.util.Calendar.getInstance(); 

    public final static void main(String[] args) throws Exception {

	if(args.length < 3) 
		System.out.println("Usage: java Worktime STARTDATE DURATION AUTHOR");

        final String STARTDATE = args[0];
	final int DURATION = Integer.parseInt(args[1]);
	final String AUTHOR = args[2];

        final FileInputStream fin = new FileInputStream(AUTHOR + ".ics");
	final CalendarBuilder builder = new CalendarBuilder();
        final Calendar calendar = builder.build(fin);

	final Period period = new Period(new DateTime(STARTDATE), new Dur(DURATION, 0, 0, 0));
	final Filter filter = new Filter(new PeriodRule(period));
	
	final Collection events = filter.filter(calendar.getComponents(Component.VEVENT));

    	final XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
    	final XMLEventWriter eventWriter = outputFactory
        	.createXMLEventWriter(new FileOutputStream(AUTHOR + ".xml"));
    	final XMLEventFactory eventFactory = XMLEventFactory.newInstance();
    	final XMLEvent end = eventFactory.createDTD("\n");
    	final StartDocument startDocument = eventFactory.createStartDocument();
    	eventWriter.add(startDocument);

    	final StartElement calendarStartElement = eventFactory.createStartElement("", "", "calendar");
    	eventWriter.add(calendarStartElement);
    	eventWriter.add(end);

	for (Iterator i = events.iterator(); i.hasNext();) {

		Component component = (Component) i.next();
    	
		StartElement eventStartElement = eventFactory.createStartElement("", "", "event");
    		eventWriter.add(eventStartElement);
    		eventWriter.add(end);

	    	for (Iterator j = component.getProperties().iterator(); j.hasNext();) {

        		Property property = (Property) j.next();
			String propertyName = property.getName();
			if(VERBOSE) System.out.println(propertyName + ": " + property.getValue());

	        	if(propertyName.equals(PROPERTIES[1])) {
    			    createNode(eventWriter, "START", parseDay(property.getValue()));
    			    createNode(eventWriter, "DTSTART", parseHour(property.getValue()));
			}
	        	if(propertyName.equals(PROPERTIES[3])) {
    			    createNode(eventWriter, "END", parseDay(property.getValue()));
    			    createNode(eventWriter, "DTEND", parseHour(property.getValue()));
			}
	        	if(propertyName.equals(PROPERTIES[4])) { 
    			    createNode(eventWriter, "LOCATION", property.getValue());
    			createNode(eventWriter, "AUTHOR", AUTHOR);
			}
	        	if(propertyName.equals(PROPERTIES[6])) {
    			    createNode(eventWriter, "SUMMARY", property.getValue());
			}
	        	if(propertyName.equals(PROPERTIES[7])) {
    			    createNode(eventWriter, "DESCRIPTION", property.getValue());
			}
			property = null;
			propertyName = null;
    		}
		component = null;
		eventWriter.add(eventFactory.createEndElement("", "", "event"));
		eventWriter.add(end);
	}

	eventWriter.add(eventFactory.createEndElement("", "", "calendar"));
	eventWriter.add(end);
	eventWriter.add(eventFactory.createEndDocument());
	eventWriter.close();
    }

    private static String parseDay(final String DATE) 
      throws java.text.ParseException {
	CAL.setTime(SDF.parse(DATE));
	return	CAL.get(java.util.Calendar.DAY_OF_MONTH) + "-" + 
 	    (CAL.get(java.util.Calendar.MONTH) +1) + "-" +
	    CAL.get(java.util.Calendar.YEAR);
    }

    private static String parseHour(final String TIME) 
      throws java.text.ParseException {
	CAL.setTime(SDF.parse(TIME));
	return CAL.get(
	    java.util.Calendar.HOUR_OF_DAY) + ":" 
	    + CAL.get(java.util.Calendar.MINUTE);
    }

    private static void createNode(XMLEventWriter eventWriter, String name,
      String value) throws XMLStreamException {

        XMLEventFactory eventFactory = XMLEventFactory.newInstance();
        XMLEvent end = eventFactory.createDTD("\n");
        XMLEvent tab = eventFactory.createDTD("\t");
        // create Start node
        StartElement sElement = eventFactory.createStartElement("", "", name);
        eventWriter.add(tab);
        eventWriter.add(sElement);
        // create Content
        Characters characters = eventFactory.createCharacters(value);
        eventWriter.add(characters);
        // create End node
        EndElement eElement = eventFactory.createEndElement("", "", name);
        eventWriter.add(eElement);
        eventWriter.add(end);
  }
}

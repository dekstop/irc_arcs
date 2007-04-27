#!/usr/bin/env ruby
#
# TODO: use median(wpm) instead of avg(wpm)?
# 
#  Created by mongo on 2006-09-29.
#  Copyright (c) 2006. All rights reserved.

require "rexml/document"

LOG_FILESPEC = "/Users/mongo/data/Colloquy Transcripts/irc.audioscrobbler.com/#last.fm*.colloquyTranscript"
#LOG_FILESPEC = "/Users/mongo/data/Colloquy Transcripts/irc.audioscrobbler.com/#last.fm 3.colloquyTranscript"

def strip_xml(str)
	str.gsub(/<[^>]+>/, '')
end

chatdata = {}

STDERR << "Reading logs ...\n"


Dir.glob(LOG_FILESPEC).each do |logfile|
	STDERR << "#{logfile} ...\n"
	data = File.read(logfile)
	REXML::Document.new(data).elements.each('log/envelope') do |el|
		user = el.get_elements('sender').first.text
		messages = el.get_elements('message')
		messages.each do |message|
			chatdata[user] = [] if (chatdata.key?(user) == false)
			chatdata[user] << strip_xml(message.to_s)
		end
  end
end


participants = chatdata.keys
STDERR << "#{participants.size} participants.\n"

xmldoc = REXML::Document.new
el_root = REXML::Element.new('irc')
xmldoc << el_root

participants.each do |user|
	
	posts = chatdata[user]

	el_user = REXML::Element.new('user')
	el_user.add_attribute('name', user)
	el_root << el_user
	
	num_words = 0
	namedrops = {}
	escaped_names = participants.map { |n| Regexp.escape(n) }
	match_names = Regexp.new("(\b#{escaped_names.join('\b|\b')}\b)")

	posts.each do |post|
		# word frequency
		words = post.split(/\b/)
		num_words += words.size
		
		# namedrops
		post.scan(match_names) do |name|
			namedrops[name] = 0 if (namedrops.key?(name) == false)
			namedrops[name] += 1
		end
	end
	
	
	el_user.add_attribute('messages', posts.size)
	el_user.add_attribute('words', num_words)
	el_user.add_attribute('words_per_message', format("%.2f", num_words.to_f / posts.size.to_f))
	
	namedrops.keys.each do |recipient|
		el_namedrop = REXML::Element.new('namedrop')
		el_namedrop.add_attribute('name', recipient)
		el_namedrop.add_attribute('count', namedrops[recipient])
		
		el_user << el_namedrop
	end
end

xml = ""
xmldoc.write(xml, 0)    # auto-indent
puts xml

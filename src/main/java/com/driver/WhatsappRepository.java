package com.driver;

import java.util.*;

import org.springframework.stereotype.Repository;

@Repository
public class WhatsappRepository {

    //Assume that each user belongs to at most one group
    //You can use the mentioned hashmaps or delete these and create your own.
    private HashMap<Group, List<User>> groupUserMap;
    private HashMap<Group, List<Message>> groupMessageMap;
    private HashMap<Message, User> senderMap;
    private HashMap<Group, User> adminMap;
    private HashSet<String> userMobile;
    private int customGroupCount;
    private int messageId;

    public WhatsappRepository() {
        this.groupMessageMap = new HashMap<Group, List<Message>>();
        this.groupUserMap = new HashMap<Group, List<User>>();
        this.senderMap = new HashMap<Message, User>();
        this.adminMap = new HashMap<Group, User>();
        this.userMobile = new HashSet<>();
        this.customGroupCount = 0;
        this.messageId = 0;
    }

    public String createUser(String name, String mobile) throws Exception {
        if (userMobile.contains(mobile)) {
            throw new Exception("User already exists");
        }

        //create user and return success
        User user = new User();
        user.setName(name);
        user.setMobile(mobile);

        //update userMobile
        userMobile.add(mobile);

        return "SUCCESS";
    }

    public Group createGroup(List<User> users) {
        Group group = new Group();
        if (users.size() < 2) {
            //group name -> second user
            group.setName(users.get(1).getName());
        }

        else {
            customGroupCount++;
            group.setName("Group " + customGroupCount);
        }

        //set other attributes
        group.setNumberOfParticipants(users.size());

        //set admin -> first user
        adminMap.put(group, users.get(0));
        return group;
    }

    public int createMessage(String content) {
        messageId++;
        Message message = new Message();
        message.setContent(content);
        return messageId;
    }

    public int sendMessage(Message message, User sender, Group group) throws Exception{
        //Throw "Group does not exist" if the mentioned group does not exist
        if(!groupUserMap.containsKey(group)) {
            throw new Exception("Group does not exist");
        }
        //Throw "You are not allowed to send message" if the sender is not a member of the group
        if(!groupUserMap.get(group).equals(sender)) {
            throw new Exception("You are not allowed to send message");
        }

        //If the message is sent successfully, return the final number of messages in that group.
        senderMap.put(message, sender);
        List<Message> newMessage = groupMessageMap.get(group);
        newMessage.add(message);
        groupMessageMap.put(group, newMessage);

        return newMessage.size();
    }

    public String changeAdmin(User approver, User user, Group group) throws Exception{
        //Throw "Group does not exist" if the mentioned group does not exist
        if(!groupUserMap.containsKey(group)) {
            throw new Exception("Group does not exist");
        }

        //Throw "Approver does not have rights" if the approver is not the current admin of the group
        if(!adminMap.get(group).equals(approver)) {
            throw new Exception("Approver does not have rights");
        }

        //Throw "User is not a participant" if the user is not a part of the group
        if(!groupUserMap.get(group).equals(user)) {
            throw new Exception("User is not a participant");
        }

        //Change the admin of the group to "user" and return "SUCCESS". Note that at one time there is only one admin and the admin rights are transferred from approver to user.
        adminMap.put(group, user);

        return "SUCCESS";
    }

    public int removeUser(User user) throws Exception{
        //A user belongs to exactly one group... (assumption)
        //search group of user
        Group group = null;
        for(Group g: groupUserMap.keySet()) {           //search if any group contain user
            if(groupMessageMap.get(g).contains(user)) { //if found
                group = g;
                break;
            }
        }

        //If user is not found in any group, throw "User not found" exception
        if(group == null) {
            throw new Exception("User not found");
        }

        //If user is found in a group, and is the admin, throw "Cannot remove admin" exception
        if(adminMap.get(group).equals(user)) {
            throw new Exception("Cannot remove admin");
        }

        //If user is not the admin, remove the user from the group, remove all its messages from all the databases, and update relevant attributes accordingly.
        //1. remove user from user map
        groupUserMap.get(group).remove(user);

        //2. remove messages from groupMessageMap
        List<Message> toBeDeleted = new ArrayList<>();

        //check all messages sent by user
        for(Message m: senderMap.keySet()) {
            if (senderMap.get(m).equals(user)) {
                //remove message of user from messageMap
                groupMessageMap.get(group).remove(m);

                //make a list
                toBeDeleted.add(m);
            }
        }

        //remove from sender map
        for(Message m: toBeDeleted) {
            senderMap.remove(m);
        }

        //If user is removed successfully, return (the updated number of users in the group + the updated number of messages in group + the updated number of overall messages)
        return groupUserMap.get(group).size() + groupMessageMap.get(group).size() + senderMap.size();
    }

    public String findMessage(Date start, Date end, int K) throws Exception{
        //tree set, rearranges them in order of time
        TreeSet<Message> messages = new TreeSet<>( (messageA, messageB) ->
                messageA.getTimestamp().compareTo(messageB.getTimestamp())
        );

        //iterate through every group
        for(Group group: groupMessageMap.keySet()) {
            //iterate through every message in list of messages, in group
            for(Message curr_message: groupMessageMap.get(group)) {

                //if the message if sent after start date, and before end date, add to treeset
                if(curr_message.getTimestamp().after(start) && curr_message.getTimestamp().before(end)) {
                    messages.add(curr_message);
                }
            }
        }

        // If the number of messages between given time is less than K, throw "K is greater than the number of messages" exception
        if(messages.size() < K) {
            throw new Exception("K is greater than the number of messages");
        }

        // Find the Kth last message between start and end (excluding start and end)
        Iterator<Message> message_pointer = messages.descendingIterator(); //iterates from last
        //iterate in reverse order, till there's a next element
        while(message_pointer.hasNext()) {
            if(K-1 > 0) { //keep moving till k-1 = 0 //next of kth element
                K--;
                message_pointer.next();
            }

            else {
                break;
            }
        }

        return message_pointer.next().getContent(); //return kth message
    }
}
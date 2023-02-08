package com.driver;

import java.util.*;

import org.springframework.stereotype.Repository;

@Repository
public class WhatsappRepository {

    //Assume that each user belongs to at most one group
    //You can use the below mentioned hashmaps or delete these and create your own.
    private HashMap<Group, List<User>> groupUserMap;        //List of users in a group
    private HashMap<Group, List<Message>> groupMessageMap;  //List of messages in a group
    private HashMap<Message, User> senderMap;               //Messages sent by each user
    private HashMap<Group, User> adminMap;                  //Admin of each group
    private HashSet<String> userMobile;                     //Mobile no of each user
    private int customGroupCount;                           //number of groups in total, members > 2
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
        //If the mobile number exists in database, throw "User already exists" exception
        //Otherwise, create the user and return "SUCCESS"
        if(userMobile.contains(mobile)) {
            throw new Exception("User already exists");
        }

        User user = new User(name, mobile);    //create new user if doesn't already exists
        userMobile.add(mobile);                //update mobile in usermobile hashset
        return "SUCCESS";
    }

    public Group createGroup(List<User> users) {
        // The list contains at least 2 users where the first user is the admin. A group has exactly one admin.
        // If there are only 2 users, the group is a personal chat and the group name should be kept as the name of the second user(other than admin)
        // If there are 2+ users, the name of group should be "Group count". For example, the name of first group would be "Group 1", second would be "Group 2" and so on.
        // Note that a personal chat is not considered a group and the count is not updated for personal chats.
        // If group is successfully created, return group.

        //For example: Consider userList1 = {Alex, Bob, Charlie}, userList2 = {Dan, Evan}, userList3 = {Felix, Graham, Hugh}.
        //If createGroup is called for these userLists in the same order, their group names would be "Group 1", "Evan", and "Group 2" respectively.

        int sizeOfGroup = users.size();                     //no of participants in the group
        String nameOfGroup = "";                            //name of the group

        if(sizeOfGroup == 2) {                              //if there are only 2 participants
            nameOfGroup = users.get(1).getName();           //group name is the name of second user
        }

        else {                                              //if there are more than 2 group members
            customGroupCount++;                             //found another group with > 2 members, so increment
            nameOfGroup = "Group " + customGroupCount;      //group name is Group _group count_
        }

        Group group = new Group(nameOfGroup, sizeOfGroup);  //create group with processed info
        groupUserMap.put(group, users);                     //update the groupusermap
        adminMap.put(group, users.get(0));                  //update admin(first user in group) in admin map

        return group;
    }

    public int createMessage(String content) {
        // The 'i^th' created message has message id 'i'.
        // Return the message id.

        messageId++;                                       //another message(string content) , so update message Id count
        Message message = new Message(messageId, content);
        return messageId;
    }

    public int sendMessage(Message message, User sender, Group group) throws Exception {
        //Throw "Group does not exist" if the mentioned group does not exist
        if(!groupUserMap.containsKey(group)) {
            throw new Exception("Group does not exist");
        }

        //Throw "You are not allowed to send message" if the sender is not a member of the group
        if(!groupUserMap.get(group).contains(sender)) {
            throw new Exception("You are not allowed to send message");
        }

        //If the message is sent successfully, return the final number of messages in that group.
        if(!groupMessageMap.containsKey(group)) {          //if group is created but this is the first message in the group
            groupMessageMap.put(group, new ArrayList<>()); //make a new entry in group message map... empty list
        }
        groupMessageMap.get(group).add(message);           //update message in group message map, get message list, and add latest message
        senderMap.put(message, sender);                    //update message in sender map

        return groupMessageMap.get(group).size();          //return no of messages in group
    }

    public String changeAdmin(User approver, User user, Group group) throws Exception {
        //Throw "Group does not exist" if the mentioned group does not exist
        if(!groupUserMap.containsKey(group)) {
            throw new Exception("Group does not exist");
        }

        //Throw "Approver does not have rights" if the approver is not the current admin of the group
        if(!adminMap.get(group).equals(approver)) {
            throw new Exception("Approver does not have rights");
        }

        //Throw "User is not a participant" if the user is not a part of the group
        if(!groupUserMap.get(group).contains(user)) {
            throw new Exception("User is not a participant");
        }

        //Change the admin of the group to "user" and return "SUCCESS".
        //Note that at one time there is only one admin and the admin rights are transferred from approver to user.
        adminMap.put(group, user);     //since its a hashmap, we cant have two keys as group, so it UPDATES the admin to user, instead of adding another

        return "SUCCESS";
    }

    public int removeUser(User user) throws Exception {
        //This is a bonus problem and does not contains any marks
        //A user belongs to exactly one group

        Group group = null;
        for(Group g: groupUserMap.keySet()) {         //search if user is present in any group
            if(groupUserMap.get(g).contains(user)) {  //user found in a group
                group = g;                            //select that group
                break;
            }
        }

        //If user is not found in any group, throw "User not found" exception
        if(group == null) {                           //user not found in any group
            throw new Exception("User not found");
        }

        //If user is found in a group and it is the admin, throw "Cannot remove admin" exception
        if(adminMap.get(group).equals(user)) {
            throw new Exception("Cannot remove admin");
        }

        //If user is not the admin, remove the user from the group,
        groupUserMap.get(group).remove(user);

        //remove all its messages from all the databases, and update relevant attributes accordingly.
        List<Message> list = new ArrayList<>();       //create empty array list of messages
        for(Message m: senderMap.keySet()) {          //traverse through sender map, messages
            if(senderMap.get(m).equals(user)) {       //if any message in sender map has user equals to given user
                groupMessageMap.get(group).remove(m); //remove it from group message map
                list.add(m);                          //add it to the list
            }
        }

        for(Message m: list) {                       //traverse through all messages in list
            senderMap.remove(m);                     //remove all from sender map
        }

        //If user is removed successfully, return (the updated number of users in the group + the updated number of messages in group + the updated number of overall messages)
        return groupUserMap.get(group).size() + groupMessageMap.get(group).size() + senderMap.size();
    }

    public String findMessage(Date start, Date end, int K) throws Exception {
        //This is a bonus problem and does not contains any marks
        // Find the Kth latest message between start and end (excluding start and end)
        // If the number of messages between given time is less than K, throw "K is greater than the number of messages" exception

        return "";
    }

}

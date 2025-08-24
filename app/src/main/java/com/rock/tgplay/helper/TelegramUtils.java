package com.rock.tgplay.helper;

import org.drinkless.tdlib.TdApi;

public class TelegramUtils {
    public static String getTagFromMsg(TdApi.Message message) {
        if (message.content instanceof TdApi.MessageText) {
            TdApi.MessageText messageText = (TdApi.MessageText) message.content;
            return messageText.text.text;
        }
        if (message.content instanceof TdApi.MessagePhoto) {
            TdApi.MessagePhoto messagePhoto = (TdApi.MessagePhoto) message.content;
            return messagePhoto.caption.text;
        }
        if (message.content instanceof TdApi.MessageAudio) {
            TdApi.MessageAudio messageAudio = (TdApi.MessageAudio) message.content;
            return messageAudio.caption.text;
        }
        if (message.content instanceof TdApi.MessageDocument) {
            TdApi.MessageDocument messageDocument = (TdApi.MessageDocument) message.content;
            return messageDocument.caption.text;
        }
        if (message.content instanceof TdApi.MessageVideo) {
            TdApi.MessageVideo messageVideo = (TdApi.MessageVideo) message.content;
            return messageVideo.caption.text;
        }
        if (message.content instanceof TdApi.MessageAnimation) {
            TdApi.MessageAnimation messageAnimation = (TdApi.MessageAnimation) message.content;
            return messageAnimation.caption.text;
        }
        if (message.content instanceof TdApi.MessageAnimatedEmoji) {

        }
        if (message.content instanceof TdApi.MessageContact) {
            TdApi.MessageContact messageContact = (TdApi.MessageContact) message.content;
            return messageContact.contact.firstName + " " + messageContact.contact.lastName;
        }
        if (message.content instanceof TdApi.MessageLocation) {
        }
        if (message.content instanceof TdApi.MessageVenue) {

        }
        if (message.content instanceof TdApi.MessageBasicGroupChatCreate) {
            TdApi.MessageBasicGroupChatCreate messageBasicGroupChatCreate = (TdApi.MessageBasicGroupChatCreate) message.content;
            return messageBasicGroupChatCreate.title;
        }
        if (message.content instanceof TdApi.MessageChatAddMembers) {
            TdApi.MessageChatAddMembers messageChatAddMembers = (TdApi.MessageChatAddMembers) message.content;
            return messageChatAddMembers.toString();
        }
        if (message.content instanceof TdApi.MessageChatJoinByLink) {
            TdApi.MessageChatJoinByLink messageChatJoinByLink = (TdApi.MessageChatJoinByLink) message.content;
            return messageChatJoinByLink.toString();
        }
        if (message.content instanceof TdApi.MessageChatDeleteMember) {
            TdApi.MessageChatDeleteMember messageChatDeleteMember = (TdApi.MessageChatDeleteMember) message.content;
            return messageChatDeleteMember.toString();
        }
        return "";
    }
}

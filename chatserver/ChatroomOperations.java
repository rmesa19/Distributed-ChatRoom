package chatserver;

import data.ChatroomDataResponse;
import data.ChatroomListResponse;
import data.IChatroomOperations;
import data.Response;
import data.ResponseStatus;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import util.ClientIPUtil;
import util.CristiansLogger;
import util.ThreadSafeStringFormatter;

/**
 * ChatRoomOperations of the server front which implements the IChatroomOperations
 * and is responsible for getting all the information related to the chatrooms at the
 * local chat server
 */
public class ChatroomOperations extends UnicastRemoteObject implements IChatroomOperations {

  private final Map<String, Chatroom> roomMap;
  private final Object roomMapLock;
  private final ServerInfo serverInfo;

  /**
   * Creates an instance of the ChatroomsOperations engine
   *
   * @param roomMap map containing the chatrooms and their names in the system
   * @param roomMapLock locks the roomMap resource
   * @param serverInfo provides port and addressing information for the server
   * @throws RemoteException if there is an error during remote communication
   */
  public ChatroomOperations(Map<String, Chatroom> roomMap, 
      Object roomMapLock, ServerInfo serverInfo) throws RemoteException {
    // the key for the room list is the name of the room, the value is the chatroom itself
    this.roomMap = roomMap;
    this.roomMapLock = roomMapLock;
    this.serverInfo = serverInfo;
  }

  /**
   * Creates a chatroom at the local chat server
   *
   * @param name name of the chatroom to create
   * @return a response indicating whether the operation has succeeded or failed
   * @throws RemoteException if there is an error during remote communication
   */
  @Override
  public Response createChatroom(String name) throws RemoteException {

    CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
        "Received createChatroom request for chatroom \"%s\"",
        name
        ));

    synchronized (roomMapLock) {
      // iterate through a list of chatroom names
      for (String roomName : roomMap.keySet()) {
        // if a chatroom already exists by this name, do not create a duplicate chatroom
        // indicate the create chatroom has failed
        if (roomName.compareTo(name) == 0) {
          CristiansLogger.writeErrorToLog(ThreadSafeStringFormatter.format(
              "Chatroom with name \"%s\" already exists",
              name
              ));
          return new Response(ResponseStatus.FAIL, "A room with the provided name already exists");
        }
      }

      // otherwise, if no existing chatroom with the same name is at 
      //this server, create new chatroom
      // with the provided name
      roomMap.put(name, new Chatroom(name));
    }

    CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
        "Successfully created chatroom \"%s\"",
        name
        ));

    return new Response(ResponseStatus.OK, "success");
  }

  /**
   * Deletes a chatroom from the local chat server
   *
   * @param name name of the chatroom to delete
   * @return a response indicating whether the operation has succeeded or failed
   * @throws RemoteException if there is an error during remote communication
   */
  @Override
  public Response deleteChatroom(String name) throws RemoteException {

    CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
        "Received deleteChatroom request for chatroom \"%s\" from node at \"%s\"",
        name,
        ClientIPUtil.getClientIP()
        ));

    // remove the chatroom using the provided name from the room map 
    //using the provided chatroom name
    synchronized (roomMapLock) {
      Chatroom r = roomMap.get(name);
      if (r != null) {
        // send message to clients that the room is closing
        r.closeRoom();
      }
      roomMap.remove(name);
    }
    return new Response(ResponseStatus.OK, "success");
  }

  /**
   * Provides information regarding the number of chatrooms and users hosted on the current
   * chat server
   *
   * @return a response containing information regarding the number of chatrooms and users hosted on the
   *         current chat server
   * @throws RemoteException if there is an error during remote communication
   */
  @Override
  public ChatroomDataResponse getChatroomData() throws RemoteException {

    CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
        "Received getChatroomData request from node at \"%s\"",
        ClientIPUtil.getClientIP()
        ));
    // get the number of chatrooms supported at this server
    int chatrooms = roomMap.size();
    int users = 0;

    // collect the number of users in each chatroom and add to the running total number of users
    // interacting with this chat server
    synchronized (roomMapLock) {
      for (String roomName : roomMap.keySet()) {
        users = users + roomMap.get(roomName).getUserCount();
      }
    }

    return new ChatroomDataResponse(chatrooms, users, 
        serverInfo.getHostname(), serverInfo.getRmiPort(), serverInfo.getTcpPort());
  }

  /**
   * Gets a list of names for chatrooms hosted at the local chat server
   *
   * @return a response containing a list of names for chatrooms hosted at the local chat server
   * @throws RemoteException if there is an error during remote communication
   */
  @Override
  public ChatroomListResponse getChatrooms() throws RemoteException {

    CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
        "Received getChatrooms request from node at \"%s\"",
        ClientIPUtil.getClientIP()
        ));

    // collect the names of all the chatrooms supported at this chat server
    List<String> chatroomNames = new LinkedList<>();
    synchronized (roomMapLock) {
      chatroomNames.addAll(roomMap.keySet());
    }
    return new ChatroomListResponse(chatroomNames);
  }

}
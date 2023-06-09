package src.commands;

import org.apache.commons.lang3.tuple.Pair;
import src.network.Request;
import src.network.Response;
import src.interfaces.CommandManagerCustom;
import src.models.Role;
import src.utils.Argument;

import java.util.List;

public class CommandBase {
    protected CommandManagerCustom commandManager;
    protected List<Role> rolesNotAllowed;
    public CommandBase(CommandManagerCustom commandManager, List<Role> rolesNotAllowed){
        this.commandManager = commandManager;
        this.rolesNotAllowed = rolesNotAllowed;
    }
    protected List<Pair<Argument, Integer>> arguments;

    protected void sendToClient(Response response, Request request){
        var data = commandManager.getSerializationManager().serialize(response);
        var executorService = commandManager.getExecutorService();
        executorService.submit(() -> commandManager.getSendingManager().send(data, request.interlayerChannel, request.clientPort));
    }
    public List<Pair<Argument, Integer>> getRequiredArguments(){
        return arguments;
    }

    public boolean isAllowedToExecute(Role role){
        return !rolesNotAllowed.contains(role);
    }
}

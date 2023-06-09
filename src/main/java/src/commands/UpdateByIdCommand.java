package src.commands;

import org.apache.commons.lang3.tuple.ImmutablePair;
import src.loggerUtils.LoggerManager;
import src.models.Product;
import org.slf4j.Logger;
import src.exceptions.CommandInterruptionException;
import src.exceptions.InterruptionCause;
import src.interfaces.Command;
import src.interfaces.CommandManagerCustom;
import src.network.MessageType;
import src.network.Request;
import src.network.Response;
import src.models.Role;
import src.service.HashingService;
import src.service.InputService;
import src.service.ValidatorService;
import src.utils.Argument;

import java.util.*;

/**
 * Class for updating the element by it`s ID
 */
public class UpdateByIdCommand extends CommandBase implements Command {
    private final InputService inputService;
    private final Logger logger;

    {
        inputService = commandManager.getInputService();
    }

    public UpdateByIdCommand(CommandManagerCustom commandManager) {
        super(commandManager, List.of(Role.MIN_USER));
        logger = LoggerManager.getLogger(UpdateByIdCommand.class);
        arguments = new LinkedList<>();
        arguments.add(ImmutablePair.of(Argument.PRODUCT, 1));
    }

    @Override
    public boolean execute(String[] args) {
        try {
            var products = commandManager.getProductsRepo().getProducts();
            var name = inputService.inputName();
            var coord = inputService.inputCoordinates();
            var price = inputService.inputPrice();
            var manufCost = inputService.inputManufactureCost();
            var unit = inputService.inputUnitOfMeasure();

            int yesOrNo = 0;
            for (; ; ) {
                try {
                    Scanner scanner = new Scanner(System.in);
                    yesOrNo = scanner.nextInt();
                    break;
                } catch (InputMismatchException e) {
                    logger.info(e.toString());
                }
            }
            var prod = new Product(Long.parseLong(args[0]), name, coord, price, manufCost,
                    unit, yesOrNo == 1 ? inputService.inputOrganization(products) : null);

            var request = new Request(MessageType.UPDATE_BY_ID);
            request.userName = args[1];
            request.userPassword = args[2];
            request.requiredArguments.add(prod);
            return execute(request);

        } catch (CommandInterruptionException e) {
            if (e.getInterruptionCause() == InterruptionCause.EXIT)
                logger.info("adding product was successfully canceled");
            else {
                logger.info("adding product was canceled by entered command");
                commandManager.executeCommand(e.getEnteredCommand());
            }
        }
        return false;
    }

    @Override
    public boolean execute(Request request) {
        var resp = new Response(null);
        var prod = (Product) request.requiredArguments.get(0);
        var id = prod.getId();
        var products = commandManager.getProductsRepo().getProducts();
        if (id <= 0) {
            resp.serverResponseToCommand = "ID must be a number greater than 0. Try typing this command again";
            sendToClient(resp, request);
            return false;
        }
        if (!ValidatorService.validateProduct(prod)) {
            resp = new Response("product has not met validation criteria");
            sendToClient(resp, request);
            return true;
        }
        logger.info("updating product with id: " + id);
        var match = products.stream().filter(p -> p.getId().equals(id)).toArray();
        if (match.length == 0) {
            resp.serverResponseToCommand = "no element with such id";
        } else {
            var hashingService = new HashingService();
            var hashedUserPassword = hashingService.hash(request.userPassword);
            var product = ((Product) match[0]);
            var user = product.getUser();
            if (!hashedUserPassword.equals(user.getPassword()) || !request.userName.equals(user.getName()))
                resp.serverResponseToCommand = String.format("product with id: %s was not updated," +
                        " because you are not the creator of that product", product.getId());
            else {
                prod.setUser(product.getUser());
                commandManager.getDbProductManager().update(prod);
                var idInCollection = products.indexOf(product);
                //commandManager.getUndoManager().logUpdateCommand(prod);
                products.remove(prod);
                products.add(idInCollection, prod);
                resp.serverResponseToCommand = "Element was updated successfully";
            }
        }
        sendToClient(resp, request);
        return true;
    }

    @Override
    public String getInfo() {
        return "update the element`s value, whose ID is equal to the given." +
                " You should enter ID after entering a command.";
    }
}

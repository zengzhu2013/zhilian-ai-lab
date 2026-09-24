package ai.agent.service;


import java.util.function.Function;

public class LocationNamesService
        implements Function<LocationNamesService.Request, LocationNamesService.Response> {


    @Override
    public Response apply(Request request) {

        String name =  request.name();
        String location =  request.location();
        if (location == null || name == null) {
            return new Response("参数缺失，无需function-call，正常响应即可..");
        }
        System.out.println(location);
        return new Response(location+"有10个！");
    }

    public record Request(
             String name,
             String location) {}
    public record Response(String message) {}


}

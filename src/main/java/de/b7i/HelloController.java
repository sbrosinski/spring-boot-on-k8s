package de.b7i;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/hello")
public class HelloController {

    @RequestMapping(method=RequestMethod.GET)
    public @ResponseBody
    Hello helloView() {
        return new Hello("hello world");
    }

}

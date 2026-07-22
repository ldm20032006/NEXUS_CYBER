package testsupport;

import demo.server.exception.DuplicateResourceException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FoundationTestController {

    @PostMapping("/test/validation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void validate(@Valid @RequestBody ValidationRequest request) {
    }

    @PostMapping("/test/duplicate")
    void duplicate() {
        throw new DuplicateResourceException("Duplicated test resource");
    }

    record ValidationRequest(@NotBlank String name) {
    }
}

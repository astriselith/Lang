
Input = {};

Input.read = (message) -> {
    println(message);
    line = readln();
    println();
    return(line);
};

export(Input);
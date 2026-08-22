
Input = {};

Input.read = fun(message): {
    println(message);
    line = readln();
    println();
    return(line);
};

export(Input);
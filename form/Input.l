
Input = {};

Input.read = fun(message): {
    print(message);
    line = readln();
    println();
    return(line);
};

export(Input);
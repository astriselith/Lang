// Input = import("./Input.l");
include("./CFlow.l");

println("Preencha algumas informações");

// name = Input.read("Nome: ");
// age = Input.read("Age: ");

read = fun(message): {
    println(message);
    line = readln();
    println();
    return(line);
};

name = read("Nome: ");
age = read("Idade: ");

try: {
	assert(name != "", "O nome não foi preenchido");
} :: (err) -> {
	println(err);
};

try: {
	assert(age != "", "A idade não foi preenchida");
} :: (err) -> {
	println(err);
};

println();
println("Completo");

println();



println();
println("Name: " + name);
println("Age: " + age);


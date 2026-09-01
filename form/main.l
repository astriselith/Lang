include("./control-flow");

println("
Preencha algumas informações:

requeridos: nome e idade.
");

read = (message, type) -> {
	finalize("return");
	return = (val) -> launch("return", val);

	# body #
    print(message);
    value = readln(type);
    return(value);
};

name = read("Nome: ", "string");
age = read("Idade: ", "int");

try: {
	assert(name != "", "O nome não foi preenchido");
} | (err) -> {
	println("Erro: @err");
};

try: {
	assert(age != 0, "A idade não foi preenchida");
} | (err) -> {
	println("Erro: @err");
};

println();

println("Completo");

println();
println("Name: @name");
println("Age: @age");







Input = import("./Input.l");
include("./CFlow.l");

println("Preencha algumas informações");

name = capture(Input.read("Nome: "));
age = capture(Input.read("Age: "));

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


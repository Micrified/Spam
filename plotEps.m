close all;
% -b=2 -l=4
epsilon = [7 5 3 1 0.5 0.2 0.1 0.005 0.0005 0.00000000001 0]
falseRej = [8 3 0 0 0 0 0 0 0  1 8]
falseAcc = [58 43 24 11 9 8 7 7 7 7 7]
plot(epsilon,falseRej)
hold on;
plot(epsilon,falseAcc)
xlabel('epsilon');
ylabel('count');
axis([-1 7 0 50]);
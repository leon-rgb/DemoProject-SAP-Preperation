import React, { useState, useEffect } from "react";
import axios from "axios";
import { Card, CardContent } from "./components/ui/card";
import { Button } from "./components/ui/button";

const BACKEND_URL = import.meta.env.VITE_BACKEND_URL || "";


export default function App() {
  const [tenant, setTenant] = useState("public");
  const [expenses, setExpenses] = useState([]);
  const [description, setDescription] = useState("");
  const [amount, setAmount] = useState("");

  const fetchExpenses = async () => {
    try {
      const response = await axios.get(`${BACKEND_URL}/expenses`, {
        headers: tenant !== "public" ? { "X-Tenant": tenant } : {}
      });
      setExpenses(response.data);
    } catch (err) {
      console.error("Error fetching expenses", err);
    }
  };

  const createExpense = async () => {
    try {
      await axios.post(
        `${BACKEND_URL}/expenses`,
        { description, amount: parseFloat(amount) },
        {
          headers: tenant !== "public" ? { "X-Tenant": tenant } : {}
        }
      );
      setDescription("");
      setAmount("");
      fetchExpenses();
    } catch (err) {
      console.error("Error creating expense", err);
    }
  };

  useEffect(() => {
    fetchExpenses();
  }, [tenant]);

  console.log(BACKEND_URL);
  return (
    <div className="p-4 max-w-xl mx-auto">
      <h1 className="text-2xl font-bold mb-4">Mini Concur Expense</h1>
      <div className="grid grid-cols-3 gap-2 mb-4">
        <input
          className="col-span-2 border rounded p-2"
          placeholder="Tenant (default public)"
          value={tenant}
          onChange={(e) => setTenant(e.target.value)}
        />
        <Button onClick={fetchExpenses}>Load</Button>
      </div>
      <Card className="mb-4">
        <CardContent>
          <h2 className="text-xl font-semibold mb-2">Add Expense</h2>
          <input
            className="w-full border rounded p-2 mb-2"
            placeholder="Description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
          <input
            className="w-full border rounded p-2 mb-2"
            placeholder="Amount"
            type="number"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
          />
          <Button onClick={createExpense}>Add</Button>
        </CardContent>
      </Card>
      <h2 className="text-xl font-semibold mb-2">Expenses</h2>
      {expenses.map((exp) => (
        <Card key={exp.id} className="mb-2">
          <CardContent>
            <div className="flex justify-between">
              <span>{exp.description}</span>
              <span>${exp.amount.toFixed(2)}</span>
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
